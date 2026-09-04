package com.phantomaddons.data;

import com.phantomaddons.PhantomAddons;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class GameplayDatabase {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    private static Connection connection = null;

    private GameplayDatabase() {}

    public static String now() { return Instant.now().toString(); }

    public static synchronized Connection get() {
        if (connection != null) return connection;
        try {
            Path dbPath = FabricLoader.getInstance().getConfigDir().resolve("phantomaddons_kuudra_data.sqlite");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA foreign_keys=ON");
                st.execute("PRAGMA busy_timeout=2000");
            }
            migrate();
            PhantomAddons.LOGGER.info("[GameplayDatabase] Opened {}", dbPath);
        } catch (SQLException e) {
            PhantomAddons.LOGGER.error("[GameplayDatabase] Failed to open database", e);
        }
        return connection;
    }

    public static synchronized void close() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException e) {
            PhantomAddons.LOGGER.error("[GameplayDatabase] Failed to close database", e);
        } finally {
            connection = null;
        }
    }

    // ── Schema migrations ────────────────────────────────────────────────────────

    private static void migrate() throws SQLException {
        int version = getUserVersion();
        if (version < 1) {
            for (String ddl : SCHEMA_V1) {
                try (Statement st = connection.createStatement()) {
                    st.execute(ddl);
                }
            }
            version = 1;
        }
        setUserVersion(version);
    }

    private static int getUserVersion() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void setUserVersion(int version) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA user_version=" + version);
        }
    }

    private static final List<String> SCHEMA_V1 = List.of(
            """
            CREATE TABLE IF NOT EXISTS runs (
                run_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at   TEXT NOT NULL,
                ended_at     TEXT,
                tier         INTEGER,
                mc_version   TEXT,
                mod_version  TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tentacle_zones (
                zone_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                label       TEXT,
                anchor_x    REAL NOT NULL,
                anchor_y    REAL NOT NULL,
                anchor_z    REAL NOT NULL,
                created_at  TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tentacle_instances (
                instance_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                run_id           INTEGER NOT NULL REFERENCES runs(run_id),
                zone_id          INTEGER REFERENCES tentacle_zones(zone_id),
                first_seen_tick  INTEGER NOT NULL,
                first_seen_at    TEXT NOT NULL,
                last_seen_tick   INTEGER,
                last_seen_at     TEXT,
                spawn_x          REAL,
                spawn_y          REAL,
                spawn_z          REAL,
                max_chain_size   INTEGER NOT NULL DEFAULT 0,
                active           INTEGER NOT NULL DEFAULT 1
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tentacle_segments (
                segment_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                instance_id      INTEGER NOT NULL REFERENCES tentacle_instances(instance_id),
                entity_uuid      TEXT NOT NULL,
                entity_type      TEXT,
                chain_position   INTEGER,
                first_seen_tick  INTEGER NOT NULL,
                last_seen_tick   INTEGER,
                despawned        INTEGER NOT NULL DEFAULT 0,
                UNIQUE(instance_id, entity_uuid)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tentacle_samples (
                sample_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                segment_id   INTEGER NOT NULL REFERENCES tentacle_segments(segment_id),
                tick         INTEGER NOT NULL,
                sampled_at   TEXT NOT NULL,
                x            REAL,
                y            REAL,
                z            REAL,
                speed        REAL,
                chain_size   INTEGER
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS supply_attempts (
                attempt_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                run_id         INTEGER NOT NULL REFERENCES runs(run_id),
                start_tick     INTEGER NOT NULL,
                started_at     TEXT NOT NULL,
                start_x        REAL,
                start_y        REAL,
                start_z        REAL,
                end_tick       INTEGER,
                ended_at       TEXT,
                outcome        TEXT,
                final_percent  INTEGER
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS tentacle_approach_events (
                event_id             INTEGER PRIMARY KEY AUTOINCREMENT,
                attempt_id           INTEGER REFERENCES supply_attempts(attempt_id),
                instance_id          INTEGER REFERENCES tentacle_instances(instance_id),
                segment_id           INTEGER REFERENCES tentacle_segments(segment_id),
                reached_tick         INTEGER NOT NULL,
                reached_at           TEXT NOT NULL,
                tentacle_x           REAL,
                tentacle_y           REAL,
                tentacle_z           REAL,
                tentacle_speed       REAL,
                tentacle_chain_size  INTEGER,
                player_x             REAL,
                player_y             REAL,
                player_z             REAL,
                player_yaw           REAL,
                player_pitch         REAL,
                resulted_in_grab     INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS grab_events (
                grab_id             INTEGER PRIMARY KEY AUTOINCREMENT,
                approach_event_id   INTEGER REFERENCES tentacle_approach_events(event_id),
                attempt_id          INTEGER REFERENCES supply_attempts(attempt_id),
                instance_id         INTEGER REFERENCES tentacle_instances(instance_id),
                mount_start_tick    INTEGER NOT NULL,
                mount_started_at    TEXT NOT NULL,
                mount_end_tick      INTEGER,
                mount_ended_at      TEXT,
                release_velocity_x  REAL,
                release_velocity_y  REAL,
                release_velocity_z  REAL,
                cancel_block_x      INTEGER,
                cancel_block_y      INTEGER,
                cancel_block_z      INTEGER,
                cancel_x            REAL,
                cancel_y            REAL,
                cancel_z            REAL,
                cancel_yaw          REAL,
                cancel_pitch        REAL,
                cancel_result       TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS pearl_throws (
                pearl_id           INTEGER PRIMARY KEY AUTOINCREMENT,
                grab_id            INTEGER REFERENCES grab_events(grab_id),
                run_id             INTEGER NOT NULL REFERENCES runs(run_id),
                tick               INTEGER NOT NULL,
                thrown_at          TEXT NOT NULL,
                player_x           REAL,
                player_y           REAL,
                player_z           REAL,
                player_yaw         REAL,
                player_pitch       REAL,
                sequence_in_grab   INTEGER,
                is_first_in_grab   INTEGER NOT NULL DEFAULT 0
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_instances_run ON tentacle_instances(run_id)",
            "CREATE INDEX IF NOT EXISTS idx_segments_instance ON tentacle_segments(instance_id)",
            "CREATE INDEX IF NOT EXISTS idx_segments_uuid ON tentacle_segments(entity_uuid)",
            "CREATE INDEX IF NOT EXISTS idx_samples_segment ON tentacle_samples(segment_id)",
            "CREATE INDEX IF NOT EXISTS idx_attempts_run ON supply_attempts(run_id)",
            "CREATE INDEX IF NOT EXISTS idx_approach_attempt ON tentacle_approach_events(attempt_id)",
            "CREATE INDEX IF NOT EXISTS idx_grab_attempt ON grab_events(attempt_id)",
            "CREATE INDEX IF NOT EXISTS idx_pearl_grab ON pearl_throws(grab_id)"
    );

    // ── Small JDBC helpers ───────────────────────────────────────────────────────

    /** Executes an INSERT and returns the generated row id, or -1 on failure. */
    public static long insert(String sql, Object... params) {
        Connection c = get();
        if (c == null) return -1;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            PhantomAddons.LOGGER.error("[GameplayDatabase] insert failed: {}", sql, e);
            return -1;
        }
    }

    public static void update(String sql, Object... params) {
        Connection c = get();
        if (c == null) return;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            PhantomAddons.LOGGER.error("[GameplayDatabase] update failed: {}", sql, e);
        }
    }

    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        List<T> out = new ArrayList<>();
        Connection c = get();
        if (c == null) return out;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapper.map(rs));
            }
        } catch (SQLException e) {
            PhantomAddons.LOGGER.error("[GameplayDatabase] query failed: {}", sql, e);
        }
        return out;
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
