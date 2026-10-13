package org.example.application;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;
import java.util.function.Consumer;
import org.example.domain.character.CharacterState;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.domain.spells.SpellSelection;

/** Character editing and protected checkpoints, independent of any Swing screen. */
public final class CharacterController implements AutoCloseable {
    private final SpellSelection selection = new SpellSelection();
    private final PlaySession session = new PlaySession(selection);
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
    private final Runnable listener = this::stateChanged;
    private CharacterState checkpoint;
    private int charisma;
    private String name;
    private boolean locked, restoring, closed;
    private Consumer<Boolean> saveLockHandler = value -> {};
    private Runnable restoreHandler = () -> {};

    public CharacterController() { this(CharacterState.defaults()); }

    public CharacterController(CharacterState initial) {
        restore(initial);
        session.addListener(listener);
        selection.addChangeListener(listener);
    }

    public PlaySession playSession() { return session; }
    public CharacterState snapshot() {
        return new CharacterState(selection.level(), charisma, selection.subclass(),
                selection.chosenSpells(), selection.species(), session.snapshot(), name);
    }
    public void restore(CharacterState state) {
        Objects.requireNonNull(state);
        restoring = true;
        try {
            charisma = state.charisma();
            name = state.name();
            selection.setCharacter(state.level(), state.subclass(), state.species());
            selection.restoreChoices(state.chosenSpells());
            session.restore(state.play());
            checkpoint = state;
        } finally { restoring = false; }
        stateChanged();
    }
    public void setLevel(int value) {
        selection.setCharacter(value, selection.subclass(), selection.species());
    }
    public void setCharismaScore(int value) {
        if (value < 1 || value > 30) throw new IllegalArgumentException("Charisma must be 1–30");
        if (charisma == value) return;
        charisma = value;
        session.characterChanged();
    }
    public void selectSubclass(SorcererRules.Subclass value) {
        selection.setCharacter(selection.level(), Objects.requireNonNull(value), selection.species());
    }
    public void selectSpecies(Species value) {
        selection.setCharacter(selection.level(), selection.subclass(), Objects.requireNonNull(value));
    }
    public String characterName() { return name; }
    public void setCharacterName(String value) {
        String clean = Objects.requireNonNull(value).strip();
        if (clean.isEmpty() || clean.length() > 60)
            throw new IllegalArgumentException("Character name must contain 1–60 characters.");
        if (clean.equals(name)) return;
        name = clean;
        stateChanged();
    }
    public boolean isSaveLocked() { return locked; }
    public void setSaveLocked(boolean value) { locked = value; stateChanged(); }
    public void requestSaveLock(boolean value) { setSaveLocked(value); saveLockHandler.accept(value); }
    public void setSaveActions(Consumer<Boolean> onLock, Runnable onRestore) {
        saveLockHandler = Objects.requireNonNull(onLock);
        restoreHandler = Objects.requireNonNull(onRestore);
    }
    public void restoreSaved() { restoreHandler.run(); }
    public void markSaved() { checkpoint = snapshot(); stateChanged(); }
    public String saveStatusText() {
        boolean changed = !snapshot().equals(checkpoint);
        return locked ? (changed ? "Experimenting · save protected" : "Saved · protected")
                : (changed ? "Changes will save on exit" : "Saved");
    }
    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        changes.addPropertyChangeListener(property, listener);
    }
    public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        changes.removePropertyChangeListener(property, listener);
    }
    private void stateChanged() {
        if (restoring || closed) return;
        changes.firePropertyChange("characterState", null, snapshot());
        changes.firePropertyChange("saveStatus", null, saveStatusText());
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        session.removeListener(listener);
        selection.removeChangeListener(listener);
        for (var listener : changes.getPropertyChangeListeners()) changes.removePropertyChangeListener(listener);
        session.dispose();
    }
}
