package lostfound.repository;

import lostfound.domain.Item;
import lostfound.exception.LostFoundException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MySQL 저장소 (JDBC). 수업에서 실습한 DriverManager + PreparedStatement 방식을 그대로 쓴다.
 *
 * <ul>
 *   <li>접속 정보는 환경변수 LF_DB_URL, LF_DB_USER, LF_DB_PASSWORD로만 받는다 (코드·저장소에 비밀번호 없음).</li>
 *   <li>처음 연결할 때 found_items 테이블이 없으면 만든다 (docs/db/schema.sql과 같은 구조).</li>
 *   <li>모든 값은 PreparedStatement의 ?로 넣어 SQL 인젝션을 막는다.</li>
 *   <li>DB 오류는 LostFoundException으로 바꿔 던진다. 시작 시 연결 실패는 RepositoryFactory가 파일 저장으로 대체한다.</li>
 * </ul>
 */
public class MySqlItemRepository implements ItemRepository {

    static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS found_items (
                id            VARCHAR(20)  NOT NULL PRIMARY KEY,
                item_type     VARCHAR(20)  NOT NULL,
                color         VARCHAR(20)  NOT NULL,
                found_date    DATE         NOT NULL,
                building      VARCHAR(30)  NOT NULL,
                floor_no      INT          NOT NULL,
                title         VARCHAR(100) NOT NULL,
                attr1         VARCHAR(100) NOT NULL DEFAULT '',
                attr2         VARCHAR(100) NOT NULL DEFAULT '',
                public_detail VARCHAR(500) NOT NULL DEFAULT '',
                secret_detail VARCHAR(500) NOT NULL DEFAULT '',
                photo_path    VARCHAR(300) NOT NULL DEFAULT '',
                status        VARCHAR(20)  NOT NULL,
                updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) DEFAULT CHARSET = utf8mb4""";

    static final String COLUMNS = "id, item_type, color, found_date, building, floor_no, title, "
            + "attr1, attr2, public_detail, secret_detail, photo_path, status";

    static final String UPSERT = "INSERT INTO found_items (" + COLUMNS + ") "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE item_type = VALUES(item_type), color = VALUES(color), "
            + "found_date = VALUES(found_date), building = VALUES(building), floor_no = VALUES(floor_no), "
            + "title = VALUES(title), attr1 = VALUES(attr1), attr2 = VALUES(attr2), "
            + "public_detail = VALUES(public_detail), secret_detail = VALUES(secret_detail), "
            + "photo_path = VALUES(photo_path), status = VALUES(status)";

    static final String SELECT_ALL = "SELECT " + COLUMNS + " FROM found_items ORDER BY id";
    static final String SELECT_ONE = "SELECT " + COLUMNS + " FROM found_items WHERE id = ?";

    private final String url;
    private final String user;
    private final String password;

    /** 연결과 테이블 준비를 바로 확인한다. 실패하면 LostFoundException. */
    public MySqlItemRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new LostFoundException("MySQL에 연결하지 못했습니다: " + e.getMessage(), e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void save(Item item) {
        String[] f = ItemCodec.fields(item);
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(UPSERT)) {
            for (int i = 0; i < f.length; i++) {
                switch (i) {
                    case 3 -> ps.setDate(i + 1, java.sql.Date.valueOf(item.getFoundDate()));
                    case 5 -> ps.setInt(i + 1, item.getFloor());
                    default -> ps.setString(i + 1, f[i]);
                }
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new LostFoundException("DB에 저장하지 못했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Item> findAll() {
        List<Item> list = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    list.add(toItem(rs));
                } catch (RuntimeException e) {
                    System.err.println("[DB 저장소] 읽을 수 없는 행 건너뜀: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new LostFoundException("DB에서 목록을 읽지 못했습니다: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<Item> findById(String id) {
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(SELECT_ONE)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(toItem(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LostFoundException("DB에서 물품을 찾지 못했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public String describe() {
        return "MySQL 저장";
    }

    private static Item toItem(ResultSet rs) throws SQLException {
        return ItemCodec.fromFields(new String[]{
                rs.getString("id"), rs.getString("item_type"), rs.getString("color"),
                rs.getDate("found_date").toLocalDate().toString(), rs.getString("building"),
                String.valueOf(rs.getInt("floor_no")), rs.getString("title"),
                rs.getString("attr1"), rs.getString("attr2"), rs.getString("public_detail"),
                rs.getString("secret_detail"), rs.getString("photo_path"), rs.getString("status")
        });
    }
}
