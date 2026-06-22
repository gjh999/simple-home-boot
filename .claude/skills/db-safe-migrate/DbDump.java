import java.sql.*;
import java.io.*;
import java.util.*;

/**
 * 복구 가능한 SQL 덤프 생성기(pg_dump 미설치 환경용 폴백).
 * - public 스키마의 모든 BASE TABLE에 대해 CREATE TABLE IF NOT EXISTS + 전체 행 INSERT 를 한 .sql 파일로 출력.
 * - PK는 재생성(데이터 복구 핵심). FK/시퀀스는 생략하므로, 정밀 복구가 필요하면 pg_dump 를 우선 사용할 것.
 *
 * 사용:
 *   javac -encoding UTF-8 DbDump.java
 *   java -cp ".;<postgres-jdbc.jar>" DbDump "<jdbcUrl>" <user> <pass> <out.sql>
 * 예) java -cp ".;C:\\Users\\me\\.m2\\...\\postgresql-42.7.3.jar" DbDump \
 *        "jdbc:postgresql://host:5432/db" db db backup_db_20260101_120000.sql
 */
public class DbDump {
    public static void main(String[] a) throws Exception {
        if (a.length < 4) { System.err.println("usage: DbDump <jdbcUrl> <user> <pass> <out.sql>"); System.exit(2); }
        String url = a[0], user = a[1], pass = a[2], out = a[3];
        try (Connection c = DriverManager.getConnection(url, user, pass);
             PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"))) {

            w.println("-- DbDump recoverable snapshot");
            w.println("-- url=" + url);
            w.println("-- NOTE: FK/시퀀스 미포함. 복구 시 빈 스키마에 적용 권장.");
            w.println("BEGIN;");
            w.println("SET session_replication_role = replica;  -- 트리거/FK 잠시 무시(복구 안정화)");
            w.println();

            List<String> tables = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(
                     "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name")) {
                while (r.next()) tables.add(r.getString(1));
            }
            System.out.println("tables: " + tables.size());

            for (String t : tables) {
                w.println("-- ===== " + t + " =====");
                w.println(buildCreate(c, t));
                dumpRows(c, t, w);
                w.println();
                System.out.println("dumped " + t);
            }

            w.println("SET session_replication_role = DEFAULT;");
            w.println("COMMIT;");
            w.flush();
            System.out.println("OK -> " + out);
        }
    }

    static String q(String id) { return "\"" + id.replace("\"", "\"\"") + "\""; }

    static String buildCreate(Connection c, String table) throws SQLException {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS " + q(table) + " (\n");
        List<String> cols = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale, " +
                "is_nullable, column_default FROM information_schema.columns " +
                "WHERE table_schema='public' AND table_name=? ORDER BY ordinal_position")) {
            ps.setString(1, table);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    String name = r.getString("column_name");
                    String type = r.getString("data_type");
                    Integer len = (Integer) r.getObject("character_maximum_length");
                    Integer prec = (Integer) r.getObject("numeric_precision");
                    Integer scale = (Integer) r.getObject("numeric_scale");
                    String nullable = r.getString("is_nullable");
                    String def = r.getString("column_default");
                    StringBuilder col = new StringBuilder("  " + q(name) + " " + type);
                    if (("character varying".equals(type) || "character".equals(type)) && len != null) col.append("(").append(len).append(")");
                    else if ("numeric".equals(type) && prec != null) col.append("(").append(prec).append(scale != null ? "," + scale : "").append(")");
                    if (def != null && !def.isBlank()) col.append(" DEFAULT ").append(def);
                    if ("NO".equals(nullable)) col.append(" NOT NULL");
                    cols.add(col.toString());
                }
            }
        }
        // PK
        List<String> pk = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT kcu.column_name FROM information_schema.table_constraints tc " +
                "JOIN information_schema.key_column_usage kcu ON tc.constraint_name=kcu.constraint_name " +
                "AND tc.table_schema=kcu.table_schema WHERE tc.constraint_type='PRIMARY KEY' " +
                "AND tc.table_schema='public' AND tc.table_name=? ORDER BY kcu.ordinal_position")) {
            ps.setString(1, table);
            try (ResultSet r = ps.executeQuery()) { while (r.next()) pk.add(q(r.getString(1))); }
        }
        sb.append(String.join(",\n", cols));
        if (!pk.isEmpty()) sb.append(",\n  PRIMARY KEY (").append(String.join(", ", pk)).append(")");
        sb.append("\n);");
        return sb.toString();
    }

    static void dumpRows(Connection c, String table, PrintWriter w) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM " + q(table))) {
            ResultSetMetaData md = r.getMetaData();
            int n = md.getColumnCount();
            StringBuilder colList = new StringBuilder();
            for (int i = 1; i <= n; i++) { if (i > 1) colList.append(", "); colList.append(q(md.getColumnLabel(i))); }
            while (r.next()) {
                StringBuilder vals = new StringBuilder();
                for (int i = 1; i <= n; i++) {
                    if (i > 1) vals.append(", ");
                    Object o = r.getObject(i);
                    if (o == null) { vals.append("NULL"); continue; }
                    int t = md.getColumnType(i);
                    if (t == Types.BOOLEAN || t == Types.BIT) { vals.append(r.getBoolean(i) ? "TRUE" : "FALSE"); continue; }
                    if (t == Types.INTEGER || t == Types.BIGINT || t == Types.SMALLINT || t == Types.TINYINT
                            || t == Types.DECIMAL || t == Types.NUMERIC || t == Types.DOUBLE || t == Types.FLOAT || t == Types.REAL) {
                        vals.append(r.getString(i)); continue; // 숫자는 따옴표 없이
                    }
                    // 그 외(문자/시각/json 등)는 따옴표 + 작은따옴표 이스케이프
                    vals.append("'").append(r.getString(i).replace("'", "''")).append("'");
                }
                w.println("INSERT INTO " + q(table) + " (" + colList + ") VALUES (" + vals + ");");
            }
        }
    }
}
