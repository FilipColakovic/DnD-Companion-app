package org.example.domain.character;

import org.example.domain.character.CharacterSheet.*;
import java.util.*;

/** PHB 2024 creation choices. Source rules and reviewed spell lists are documented in CHARACTER_CHOICES.md. */
public final class CharacterChoices {
    private CharacterChoices(){}
    public record Choice(String id,String title,String explanation,int count,List<String> options) {
        public Choice { options=List.copyOf(options); }
        public boolean accepts(List<String> selected){return selected.size()==count && new HashSet<>(selected).size()==count && options.containsAll(selected);}
        public boolean complete(CharacterSheet sheet){return accepts(sheet.choice(id));}
    }
    private static List<String> names(String data){return List.of(data.split("\\|"));}
    private static final List<String> SKILLS=Arrays.stream(Skill.values()).map(skill->skill.label).toList();
    private static final List<String> ABILITIES=List.of("Intelligence","Wisdom","Charisma");
    private static final List<String> ARTISAN=names("Alchemist's Supplies|Brewer's Supplies|Calligrapher's Supplies|Carpenter's Tools|Cartographer's Tools|Cobbler's Tools|Cook's Utensils|Glassblower's Tools|Jeweler's Tools|Leatherworker's Tools|Mason's Tools|Painter's Supplies|Potter's Tools|Smith's Tools|Tinker's Tools|Weaver's Tools|Woodcarver's Tools");
    private static final List<String> INSTRUMENTS=names("Bagpipes|Drum|Dulcimer|Flute|Horn|Lute|Lyre|Pan Flute|Shawm|Viol");
    private static List<String> tools(){List<String> list=new ArrayList<>(ARTISAN);list.addAll(INSTRUMENTS);list.addAll(names("Dice Set|Dragonchess Set|Playing Card Set|Three-Dragon Ante Set|Disguise Kit|Forgery Kit|Herbalism Kit|Navigator's Tools|Poisoner's Kit|Thieves' Tools"));return List.copyOf(list);}
    private static List<String> skilledOptions(){List<String> list=new ArrayList<>(SKILLS);tools().forEach(tool->list.add("Tool: "+tool));return List.copyOf(list);}
    private static final Map<String,List<String>> CANTRIPS=Map.of(
        "Cleric",names("Guidance|Light|Mending|Resistance|Sacred Flame|Spare the Dying|Thaumaturgy|Toll the Dead|Word of Radiance"),
        "Druid",names("Druidcraft|Elementalism|Guidance|Mending|Message|Poison Spray|Produce Flame|Resistance|Shillelagh|Spare the Dying|Starry Wisp|Thorn Whip|Thunderclap"),
        "Wizard",names("Acid Splash|Blade Ward|Chill Touch|Dancing Lights|Elementalism|Fire Bolt|Friends|Light|Mage Hand|Mending|Message|Mind Sliver|Minor Illusion|Poison Spray|Prestidigitation|Ray of Frost|Shocking Grasp|Thunderclap|Toll the Dead|True Strike"));
    private static final Map<String,List<String>> SPELLS=Map.of(
        "Cleric",names("Bane|Bless|Command|Create or Destroy Water|Cure Wounds|Detect Evil and Good|Detect Magic|Detect Poison and Disease|Guiding Bolt|Healing Word|Inflict Wounds|Protection from Evil and Good|Purify Food and Drink|Sanctuary|Shield of Faith"),
        "Druid",names("Animal Friendship|Charm Person|Create or Destroy Water|Cure Wounds|Detect Magic|Detect Poison and Disease|Entangle|Faerie Fire|Fog Cloud|Goodberry|Healing Word|Ice Knife|Jump|Longstrider|Protection from Evil and Good|Purify Food and Drink|Speak with Animals|Thunderwave"),
        "Wizard",names("Alarm|Burning Hands|Charm Person|Chromatic Orb|Color Spray|Comprehend Languages|Detect Magic|Disguise Self|Expeditious Retreat|False Life|Feather Fall|Find Familiar|Fog Cloud|Grease|Ice Knife|Identify|Illusory Script|Jump|Longstrider|Mage Armor|Magic Missile|Protection from Evil and Good|Ray of Sickness|Shield|Silent Image|Sleep|Tasha's Hideous Laughter|Tenser's Floating Disk|Thunderwave|Unseen Servant|Witch Bolt"));
    public static final Choice SORCERER_SKILLS=new Choice("sorcerer.skills","Sorcerer · Skill proficiencies","Choose two class skills. These choices grant proficiency, not expertise.",2,names("Arcana|Deception|Insight|Intimidation|Persuasion|Religion"));
    public static final Choice ELF_SKILL=new Choice("elf.skill","Elf · Keen Senses","Choose one skill proficiency.",1,names("Insight|Perception|Survival"));
    public static final Choice HUMAN_SKILL=new Choice("human.skill","Human · Skillful","Choose one additional skill proficiency.",1,SKILLS);
    public static final Choice HUMAN_FEAT=new Choice("human.feat","Human · Versatile","Choose one PHB 2024 Origin feat, in addition to your background feat.",1,FeatCatalog.all().stream().filter(feat->feat.category()==FeatCatalog.Category.ORIGIN).map(FeatCatalog.Feat::name).toList());
    public static final Choice DRAGON=new Choice("dragonborn.ancestry","Dragonborn · Draconic Ancestry","Choose an ancestor. This determines your breath weapon damage and damage resistance.",1,names("Black — Acid|Blue — Lightning|Brass — Fire|Bronze — Lightning|Copper — Acid|Gold — Fire|Green — Poison|Red — Fire|Silver — Cold|White — Cold"));
    public static final Choice GIANT=new Choice("goliath.ancestry","Goliath · Giant Ancestry","Choose one ancestral benefit.",1,names("Cloud — Cloud's Jaunt|Fire — Fire's Burn|Frost — Frost's Chill|Hill — Hill's Tumble|Stone — Stone's Endurance|Storm — Storm's Thunder"));
    public static final Choice SKILLED=new Choice("feat.Skilled","Skilled · Proficiencies","Choose any combination of three skills or tools.",3,skilledOptions());
    public static final Choice CRAFTER=new Choice("feat.Crafter","Crafter · Tool proficiencies","Choose three different Artisan's Tools.",3,ARTISAN);
    public static final Choice MUSICIAN=new Choice("feat.Musician","Musician · Instrument proficiencies","Choose three different musical instruments.",3,INSTRUMENTS);
    public static final Choice SKILL_EXPERT=new Choice("feat.Skill Expert.skill","Skill Expert · Proficiency","Choose one skill proficiency.",1,SKILLS);
    public static final Choice KEEN_MIND=new Choice("feat.Keen Mind","Keen Mind · Lore Knowledge","Choose one skill. Gain proficiency, or expertise if already proficient.",1,names("Arcana|History|Investigation|Nature|Religion"));
    public static final Choice OBSERVANT=new Choice("feat.Observant","Observant · Keen Observer","Choose one skill. Gain proficiency, or expertise if already proficient.",1,names("Insight|Investigation|Perception"));
    private static final Choice INITIATE_LIST=new Choice("feat.Magic Initiate.list","Magic Initiate · Spell list","Choose one list for all three spells.",1,List.of("Cleric","Druid","Wizard"));
    private static final Choice INITIATE_ABILITY=new Choice("feat.Magic Initiate.ability","Magic Initiate · Spellcasting ability","Choose the ability used for these spells.",1,ABILITIES);
    private static final Choice BACKGROUND=new Choice("background","Character · Background","Choose a PHB 2024 background in Feats.",1,BackgroundCatalog.all().stream().map(BackgroundCatalog.Background::name).toList());
    private static final List<String> GAMING=names("Dice Set|Dragonchess Set|Playing Card Set|Three-Dragon Ante Set");
    public static boolean elf(Species species){return species==Species.DROW || species==Species.HIGH_ELF || species==Species.WOOD_ELF;}
    public static boolean castingChoice(Species species){return elf(species) || species==Species.FOREST_GNOME || species==Species.ROCK_GNOME || tiefling(species);}
    private static boolean tiefling(Species species){return species==Species.ABYSSAL_TIEFLING || species==Species.CHTHONIC_TIEFLING || species==Species.INFERNAL_TIEFLING;}
    private static boolean sizeChoice(Species species){return species==Species.HUMAN || species==Species.AASIMAR || tiefling(species);}
    private static Choice size(Species species){return new Choice("species."+species.name()+".size",species+" · Size","Choose your character's size.",1,List.of("Small","Medium"));}
    public static Choice casting(Species species){return new Choice("species."+species.name()+".casting",species+" · Spellcasting ability","Choose the ability for spells granted by this lineage or legacy.",1,ABILITIES);}
    public static List<Choice> speciesChoices(Species species){
        List<Choice> list=new ArrayList<>();
        if(sizeChoice(species))list.add(size(species));
        if(species==Species.HUMAN){list.add(HUMAN_SKILL);list.add(HUMAN_FEAT);}
        if(elf(species))list.add(ELF_SKILL);
        if(castingChoice(species))list.add(casting(species));
        if(species==Species.DRAGONBORN)list.add(DRAGON);
        if(species==Species.GOLIATH)list.add(GIANT);
        return List.copyOf(list);
    }
    public static List<Choice> required(CharacterSheet sheet,Species species,Set<String> feats){
        List<Choice> list=new ArrayList<>();list.add(SORCERER_SKILLS);list.addAll(speciesChoices(species));
        if(species==Species.HUMAN){
            String backgroundFeat=BackgroundCatalog.selected(sheet).map(BackgroundCatalog.Background::feat).orElse("");
            List<String> allowed=HUMAN_FEAT.options().stream().filter(name->!name.equals(backgroundFeat)||FeatCatalog.named(name).repeatable()).toList();
            int index=list.indexOf(HUMAN_FEAT);list.set(index,new Choice(HUMAN_FEAT.id(),HUMAN_FEAT.title(),HUMAN_FEAT.explanation(),1,allowed));
        }
        Set<String> chosen=new HashSet<>(feats);if(species==Species.HUMAN)chosen.addAll(sheet.choice("human.feat"));
        for(String feat:chosen)addFeatChoices(list,sheet,feat,"feat."+feat,"","");
        if(chosen.contains("Skill Expert")){
            list.add(SKILL_EXPERT);Set<Skill> granted=skills(sheet,species,feats);
            Set<String> otherFeats=new HashSet<>(feats);otherFeats.remove("Skill Expert");Set<Skill> otherExpertise=expertise(sheet,species,otherFeats);
            List<String> eligible=Arrays.stream(Skill.values()).filter(skill->sheet.tier(skill)>0||granted.contains(skill))
                .filter(skill->!otherExpertise.contains(skill))
                .filter(skill->sheet.tier(skill)<2||sheet.choice("feat.Skill Expert.expertise").contains(skill.label)).map(skill->skill.label).toList();
            list.add(new Choice("feat.Skill Expert.expertise","Skill Expert · Expertise","Choose a proficient skill without expertise. Choose your new proficiency first if necessary.",1,eligible));
        }
        if(chosen.contains("Keen Mind"))list.add(KEEN_MIND);
        if(chosen.contains("Observant"))list.add(OBSERVANT);
        BackgroundCatalog.selected(sheet).ifPresent(background->list.addAll(backgroundChoices(background,sheet)));
        return List.copyOf(list);
    }
    private static void addFeatChoices(List<Choice> list,CharacterSheet sheet,String feat,String prefix,String fixedList,String source){
        Choice base=switch(feat){case "Skilled"->SKILLED;case "Crafter"->CRAFTER;case "Musician"->MUSICIAN;default->null;};
        if(base!=null)list.add(new Choice(prefix,source+base.title(),base.explanation(),base.count(),base.options()));
        if(feat.equals("Magic Initiate")){
            List<String> permitted=INITIATE_LIST.options();
            if(fixedList.isEmpty()&&source.isEmpty()){
                String backgroundList=BackgroundCatalog.selected(sheet).map(BackgroundCatalog.Background::spellList).orElse("");
                permitted=permitted.stream().filter(name->!name.equals(backgroundList)).toList();
            }
            if(fixedList.isEmpty())list.add(new Choice(prefix+".list",source+INITIATE_LIST.title(),INITIATE_LIST.explanation(),1,permitted));
            list.add(new Choice(prefix+".ability",source+INITIATE_ABILITY.title(),INITIATE_ABILITY.explanation(),1,ABILITIES));
            String spellList=fixedList.isEmpty()?sheet.choice(prefix+".list").stream().findFirst().orElse(""):fixedList;
            if(fixedList.isEmpty()&&!permitted.contains(spellList))spellList="";
            if(CANTRIPS.containsKey(spellList)){
                list.add(new Choice(prefix+".cantrips",source+"Magic Initiate · Two cantrips","Choose two different "+spellList+" cantrips.",2,CANTRIPS.get(spellList)));
                list.add(new Choice(prefix+".spell",source+"Magic Initiate · Level 1 spell","Choose one level 1 "+spellList+" spell.",1,SPELLS.get(spellList)));
            }
        }
    }
    public static List<Choice> backgroundChoices(BackgroundCatalog.Background background,CharacterSheet sheet){
        List<Choice> list=new ArrayList<>();String prefix=background.id();
        List<String> allocations=new ArrayList<>();allocations.add(String.join(" / ",background.abilities().stream().map(ability->ability+" +1").toList()));
        for(Ability two:background.abilities())for(Ability one:background.abilities())if(two!=one)allocations.add(two+" +2 / "+one+" +1");
        list.add(new Choice(prefix+".abilities",background.name()+" · Ability increases","Record +2/+1 or +1 to all three. Enter final ability scores on your sheet; this selection does not add the increases again.",1,allocations));
        List<String> tools=switch(background.tool()){case "Artisan's Tools"->ARTISAN;case "Musical Instrument"->INSTRUMENTS;case "Gaming Set"->GAMING;default->List.of();};
        if(!tools.isEmpty())list.add(new Choice(prefix+".tool",background.name()+" · Tool proficiency","Choose one "+background.tool()+" proficiency.",1,tools));
        list.add(new Choice(prefix+".equipment",background.name()+" · Starting equipment","Record your starting-equipment option. Equipment and gold are managed on your sheet.",1,List.of("Equipment package","50 GP")));
        if(background.name().equals("Wayfarer")&&sheet.choice(prefix+".equipment").contains("Equipment package"))list.add(new Choice(prefix+".gaming",background.name()+" · Equipment gaming set","Choose the gaming set in your equipment package; this does not grant proficiency.",1,GAMING));
        addFeatChoices(list,sheet,background.feat(),prefix+".feat."+background.feat(),background.spellList(),background.name()+" · ");return List.copyOf(list);
    }
    public static void validateStored(String id,String value){
        List<Choice> known=new ArrayList<>(List.of(BACKGROUND,SORCERER_SKILLS,ELF_SKILL,HUMAN_SKILL,HUMAN_FEAT,DRAGON,GIANT,SKILLED,CRAFTER,MUSICIAN,INITIATE_LIST,INITIATE_ABILITY,SKILL_EXPERT,KEEN_MIND,OBSERVANT));
        known.add(new Choice("feat.Skill Expert.expertise","","",1,SKILLS));
        known.add(new Choice("feat.Keen Mind.tier","","",1,List.of("Proficiency","Expertise")));known.add(new Choice("feat.Observant.tier","","",1,List.of("Proficiency","Expertise")));
        for(Species species:Species.values())known.addAll(speciesChoices(species));
        known.add(new Choice("feat.Magic Initiate.cantrips","","",2,CANTRIPS.values().stream().flatMap(List::stream).distinct().toList()));
        known.add(new Choice("feat.Magic Initiate.spell","","",1,SPELLS.values().stream().flatMap(List::stream).distinct().toList()));
        for(var background:BackgroundCatalog.all())known.addAll(backgroundChoices(background,CharacterSheet.defaults()));
        known.add(new Choice("background.Wayfarer.gaming","","",1,GAMING));
        Choice choice=known.stream().filter(item->item.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown creation choice: "+id));
        if(!value.isEmpty() && !choice.accepts(List.of(value.split("\\|",-1))))throw new IllegalArgumentException("Invalid selection for "+id);
    }
    public static Map<String,String> feats(CharacterSheet sheet,Species species,Map<String,String> manual){
        Map<String,String> result=new HashMap<>(manual);
        if(species==Species.HUMAN && HUMAN_FEAT.complete(sheet))result.putIfAbsent(sheet.choice("human.feat").getFirst(),"Human · Versatile");
        BackgroundCatalog.selected(sheet).ifPresent(background->{String notes=sheet.note(Note.BACKGROUND_FEAT);result.putIfAbsent(background.feat(),notes.isBlank()?"Background · "+background.name():notes);});
        return Map.copyOf(result);
    }
    public static Set<Skill> skills(CharacterSheet sheet,Species species,Set<String> feats){
        Set<Skill> result=EnumSet.noneOf(Skill.class);
        Set<String> active=new HashSet<>(feats);if(species==Species.HUMAN)active.addAll(sheet.choice("human.feat"));
        for(Choice choice:List.of(SORCERER_SKILLS,HUMAN_SKILL,ELF_SKILL,SKILLED)){
            if(choice==HUMAN_SKILL && species!=Species.HUMAN || choice==ELF_SKILL && !elf(species) || choice==SKILLED && !active.contains("Skilled"))continue;
            if(choice.complete(sheet))for(String label:sheet.choice(choice.id()))for(Skill skill:Skill.values())if(skill.label.equals(label))result.add(skill);
        }
        BackgroundCatalog.selected(sheet).ifPresent(background->{
            result.addAll(background.skills());
            if(background.feat().equals("Skilled"))for(String label:sheet.choice(background.id()+".feat.Skilled"))for(Skill skill:Skill.values())if(skill.label.equals(label))result.add(skill);
        });
        for(Choice choice:List.of(SKILL_EXPERT,KEEN_MIND,OBSERVANT)){
            String feat=choice==SKILL_EXPERT?"Skill Expert":choice==KEEN_MIND?"Keen Mind":"Observant";
            if(active.contains(feat)&&choice.complete(sheet))for(Skill skill:Skill.values())if(sheet.choice(choice.id()).contains(skill.label))result.add(skill);
        }
        return Set.copyOf(result);
    }
    public static Set<Skill> expertise(CharacterSheet sheet,Species species,Set<String> feats){
        Set<Skill> result=EnumSet.noneOf(Skill.class),granted=skills(sheet,species,feats);
        if(feats.contains("Skill Expert"))for(Skill skill:Skill.values())if(sheet.choice("feat.Skill Expert.expertise").contains(skill.label)&&(granted.contains(skill)||sheet.tier(skill)>0))result.add(skill);
        for(Choice choice:List.of(KEEN_MIND,OBSERVANT))if(feats.contains(choice==KEEN_MIND?"Keen Mind":"Observant")&&sheet.choice(choice.id()+".tier").contains("Expertise"))for(Skill skill:Skill.values())if(sheet.choice(choice.id()).contains(skill.label))result.add(skill);
        return Set.copyOf(result);
    }
    public static Ability speciesAbility(CharacterSheet sheet,Species species){
        String label=sheet.choice(casting(species).id()).stream().findFirst().orElse("Charisma");
        return label.equals("Intelligence")?Ability.INT:label.equals("Wisdom")?Ability.WIS:Ability.CHA;
    }
    public static String speciesSummary(CharacterSheet sheet,Species species){
        return speciesChoices(species).stream().filter(choice->choice.complete(sheet))
            .map(choice->choice.title().substring(choice.title().indexOf('·')+1).strip()+": "+String.join(", ",sheet.choice(choice.id())))
            .collect(java.util.stream.Collectors.joining("\n"));
    }
    public static List<String> grantedTools(CharacterSheet sheet,Set<String> feats){
        Set<String> result=new TreeSet<>();
        if(feats.contains("Skilled")&&SKILLED.complete(sheet))sheet.choice(SKILLED.id()).stream().filter(item->item.startsWith("Tool: ")).map(item->item.substring(6)).forEach(result::add);
        if(feats.contains("Crafter")&&CRAFTER.complete(sheet))result.addAll(sheet.choice(CRAFTER.id()));
        if(feats.contains("Musician")&&MUSICIAN.complete(sheet))result.addAll(sheet.choice(MUSICIAN.id()));
        BackgroundCatalog.selected(sheet).ifPresent(background->{
            if(Set.of("Artisan's Tools","Musical Instrument","Gaming Set").contains(background.tool()))result.addAll(sheet.choice(background.id()+".tool"));else result.add(background.tool());
            for(String tool:sheet.choice(background.id()+".feat."+background.feat()))if(tool.startsWith("Tool: "))result.add(tool.substring(6));else if(background.feat().equals("Crafter")||background.feat().equals("Musician"))result.add(tool);
        });
        return List.copyOf(result);
    }
}
