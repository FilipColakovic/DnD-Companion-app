package org.example.ui.character;

import org.example.domain.character.SorcererRules.Subclass;
import org.example.ui.common.PillSelector;
import java.util.List;
import java.util.function.Consumer;

public final class SubclassSelector extends PillSelector<Subclass> {
    public SubclassSelector(Consumer<Subclass> onSelect) {
        super(List.of(Subclass.values()), Subclass.ABERRANT, onSelect);
        getAccessibleContext().setAccessibleName("Select Sorcerer subclass");
        setToolTipText("Choose a subclass from the 2024 Player's Handbook");
    }
    public void setSubclass(Subclass subclass) { setSelectedOption(subclass); }
}