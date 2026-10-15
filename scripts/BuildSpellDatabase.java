import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

/** Offline catalog builder. Run Generate-SpellDetails.ps1; no application classes are required. */
public class BuildSpellDatabase {
    static String decode(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                if (++i == value.length()) throw new IllegalArgumentException("Incomplete escape");
                c =
                        switch (value.charAt(i)) {
                            case 'n' -> '\n';
                            case 'r' -> '\r';
                            case 't' -> '\t';
                            case '\\' -> '\\';
                            default -> throw new IllegalArgumentException("Unknown escape");
                        };
            }
            result.append(c);
        }
        return result.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2)
            throw new IllegalArgumentException("Expected source TSV and output database paths");
        Path output = Path.of(args[1]).toAbsolutePath();
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), "spells-build-", ".db");
        try {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + temporary)) {
                try (Statement pragma = connection.createStatement()) {
                    pragma.execute("PRAGMA foreign_keys=ON");
                }
                connection.setAutoCommit(false);
                try (Statement schema = connection.createStatement()) {
                    schema.executeUpdate(
                            """
                            CREATE TABLE spells (
                              name TEXT PRIMARY KEY NOT NULL CHECK(length(name)>0),
                              level INTEGER NOT NULL CHECK(level BETWEEN 0 AND 9),
                              sorcerer_spell INTEGER NOT NULL CHECK(sorcerer_spell IN (0,1)),
                              school TEXT NOT NULL, casting_time TEXT NOT NULL, range TEXT NOT NULL,
                              components TEXT NOT NULL, duration TEXT NOT NULL,
                              concentration INTEGER NOT NULL CHECK(concentration IN (0,1)),
                              ritual INTEGER NOT NULL CHECK(ritual IN (0,1)),
                              description TEXT NOT NULL, higher_levels TEXT NOT NULL,
                              source TEXT NOT NULL, reference TEXT NOT NULL
                            )
                            """);
                    schema.executeUpdate(
                            "CREATE TABLE spell_classes (spell_name TEXT NOT NULL REFERENCES"
                                    + " spells(name), class_id TEXT NOT NULL, PRIMARY"
                                    + " KEY(spell_name,class_id))");
                    schema.executeUpdate(
                            "CREATE TABLE spell_checks (spell_name TEXT PRIMARY KEY REFERENCES"
                                + " spells(name), attack INTEGER NOT NULL CHECK(attack IN (0,1)),"
                                + " saves TEXT NOT NULL)");
                    schema.executeUpdate("PRAGMA user_version=2");
                }
                List<String> rows = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
                String expected =
                        "name\tlevel\tsorcerer_spell\tschool\tcasting_time\trange\tcomponents"
                                + "\tduration\tconcentration\tritual\tdescription\thigher_levels"
                                + "\tsource\treference";
                if (rows.size() < 2 || !rows.getFirst().equals(expected))
                    throw new IllegalArgumentException(
                            "Expected catalog columns and at least one spell");
                try (PreparedStatement insert =
                        connection.prepareStatement(
                                "INSERT INTO spells VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    for (int row = 1; row < rows.size(); row++) {
                        String[] fields = rows.get(row).split("\t", -1);
                        if (fields.length != 14)
                            throw new IllegalArgumentException(
                                    "Wrong column count on line " + (row + 1));
                        for (int column = 0; column < 14; column++) {
                            String value = decode(fields[column]);
                            if (Set.of(0, 3, 4, 5, 7, 10, 12, 13).contains(column)
                                    && value.isBlank())
                                throw new IllegalArgumentException(
                                        "Empty required field on line " + (row + 1));
                            if (column == 13) {
                                var reference = java.net.URI.create(value);
                                if (!"https".equals(reference.getScheme())
                                        || reference.getHost() == null)
                                    throw new IllegalArgumentException(
                                            "Reference must be HTTPS on line " + (row + 1));
                            }
                            if (column == 1) insert.setInt(column + 1, Integer.parseInt(value));
                            else if (column == 2 || column == 8 || column == 9) {
                                if (!value.equals("true") && !value.equals("false"))
                                    throw new IllegalArgumentException(
                                            "Invalid boolean on line " + (row + 1));
                                insert.setInt(column + 1, Boolean.parseBoolean(value) ? 1 : 0);
                            } else insert.setString(column + 1, value);
                        }
                        insert.executeUpdate();
                    }
                }
                List<String> metadata =
                        Files.readAllLines(
                                Path.of(args[0])
                                        .toAbsolutePath()
                                        .getParent()
                                        .resolve("spell-mechanics.tsv"),
                                StandardCharsets.UTF_8);
                if (!metadata.getFirst().equals("name\tclasses\tattack\tsaves")
                        || metadata.size() != rows.size())
                    throw new IllegalArgumentException(
                            "Spell metadata must cover every catalog entry");
                try (var classes =
                                connection.prepareStatement(
                                        "INSERT INTO spell_classes VALUES (?,?)");
                        var checks =
                                connection.prepareStatement(
                                        "INSERT INTO spell_checks VALUES (?,?,?)")) {
                    for (String line : metadata.subList(1, metadata.size())) {
                        String[] fields = line.split("\t", -1);
                        if (fields.length != 4
                                || fields[1].isBlank()
                                || !Set.of("true", "false").contains(fields[2]))
                            throw new IllegalArgumentException(
                                    "Invalid spell metadata row: " + line);
                        for (String cls : fields[1].split("\\|")) {
                            if (!Set.of(
                                            "BARD",
                                            "CLERIC",
                                            "DRUID",
                                            "PALADIN",
                                            "RANGER",
                                            "SORCERER",
                                            "WARLOCK",
                                            "WIZARD")
                                    .contains(cls))
                                throw new IllegalArgumentException("Invalid spell class");
                            classes.setString(1, fields[0]);
                            classes.setString(2, cls);
                            classes.executeUpdate();
                        }
                        if (!fields[3].isEmpty())
                            for (String save : fields[3].split("\\|"))
                                if (!Set.of("STR", "DEX", "CON", "INT", "WIS", "CHA")
                                        .contains(save))
                                    throw new IllegalArgumentException("Invalid save ability");
                        checks.setString(1, fields[0]);
                        checks.setBoolean(2, Boolean.parseBoolean(fields[2]));
                        checks.setString(3, fields[3]);
                        checks.executeUpdate();
                    }
                }
                connection.commit();
                System.out.println("Built " + (rows.size() - 1) + " spells: " + output);
            }
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
