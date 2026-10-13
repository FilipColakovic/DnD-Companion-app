package org.example.domain.character;

import org.example.domain.character.CharacterSheet.Ability;
import org.example.domain.character.CharacterSheet.Skill;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** The sixteen PHB 2024 backgrounds; short descriptions are original summaries. */
public final class BackgroundCatalog {
    private BackgroundCatalog() {}

    public record Background(
            String name,
            List<Ability> abilities,
            String feat,
            String spellList,
            List<Skill> skills,
            String tool,
            String summary) {
        public Background {
            abilities = List.copyOf(abilities);
            skills = List.copyOf(skills);
        }

        public String id() {
            return "background." + name;
        }

        public String sourceUrl() {
            return "https://roll20.net/compendium/dnd5e/Backgrounds%3A" + name + "?expansion=32231";
        }

        public String featLabel() {
            return feat + (spellList.isEmpty() ? "" : " (" + spellList + ")");
        }
    }

    private static final List<Background> ALL =
            BackgroundCatalog.DATA
                    .lines()
                    .filter(line -> !line.isBlank())
                    .map(
                            line -> {
                                String[] fields = line.split("\\|", -1);
                                return new Background(
                                        fields[0],
                                        Arrays.stream(fields[1].split(","))
                                                .map(Ability::valueOf)
                                                .toList(),
                                        fields[2],
                                        fields[3],
                                        Arrays.stream(fields[4].split(","))
                                                .map(Skill::valueOf)
                                                .toList(),
                                        fields[5],
                                        fields[6]);
                            })
                    .toList();

    public static List<Background> all() {
        return ALL;
    }

    public static Background named(String name) {
        return ALL.stream()
                .filter(background -> background.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PHB background: " + name));
    }

    public static Optional<Background> selected(CharacterSheet sheet) {
        return sheet.choice("background").stream().findFirst().map(BackgroundCatalog::named);
    }

    private static final String DATA =
            """
            Acolyte|INT,WIS,CHA|Magic Initiate|Cleric|INSIGHT,RELIGION|Calligrapher's Supplies|Temple service, religious study and ritual practice.
            Artisan|STR,DEX,INT|Crafter||INVESTIGATION,PERSUASION|Artisan's Tools|Workshop training, craftsmanship and working with customers.
            Charlatan|DEX,CON,CHA|Skilled||DECEPTION,SLEIGHT_OF_HAND|Forgery Kit|False identities, convincing stories and practiced misdirection.
            Criminal|DEX,CON,INT|Alert||SLEIGHT_OF_HAND,STEALTH|Thieves' Tools|Quiet movement and practical experience in the criminal world.
            Entertainer|STR,DEX,CHA|Musician||ACROBATICS,PERFORMANCE|Musical Instrument|Stagecraft, music and learning to hold an audience.
            Farmer|STR,CON,WIS|Tough||ANIMAL_HANDLING,NATURE|Carpenter's Tools|Rural work, tending animals and enduring physical labor.
            Guard|STR,INT,WIS|Alert||ATHLETICS,PERCEPTION|Gaming Set|Watch duty, physical training and attention to trouble.
            Guide|DEX,CON,WIS|Magic Initiate|Druid|STEALTH,SURVIVAL|Cartographer's Tools|Wilderness travel, navigation and an affinity for nature.
            Hermit|CON,WIS,CHA|Healer||MEDICINE,RELIGION|Herbalism Kit|Solitary reflection, healing knowledge and spiritual study.
            Merchant|CON,INT,CHA|Lucky||ANIMAL_HANDLING,PERSUASION|Navigator's Tools|Trade, travel and negotiating with people and suppliers.
            Noble|STR,INT,CHA|Skilled||HISTORY,PERSUASION|Gaming Set|Courtly education, influence and experience with leadership.
            Sage|CON,INT,WIS|Magic Initiate|Wizard|ARCANA,HISTORY|Calligrapher's Supplies|Scholarly research, historical knowledge and arcane study.
            Sailor|STR,DEX,WIS|Tavern Brawler||ACROBATICS,PERCEPTION|Navigator's Tools|Shipboard life, navigation and keeping your footing at sea.
            Scribe|DEX,INT,WIS|Skilled||INVESTIGATION,PERCEPTION|Calligrapher's Supplies|Careful records, detailed observation and written communication.
            Soldier|STR,DEX,CON|Savage Attacker||ATHLETICS,INTIMIDATION|Gaming Set|Military drills, combat training and a commanding presence.
            Wayfarer|DEX,WIS,CHA|Lucky||INSIGHT,STEALTH|Thieves' Tools|Life on the move, reading people and staying unnoticed.
            """;
}
