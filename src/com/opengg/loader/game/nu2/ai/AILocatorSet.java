package com.opengg.loader.game.nu2.ai;

import com.opengg.core.engine.OpenGG;
import com.opengg.loader.EditorEntity;
import com.opengg.loader.MapEntity;
import com.opengg.loader.Util;
import com.opengg.loader.loading.MapWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record AILocatorSet(String name, List<AILocator> locators, int fileAddress, int endAddress) implements MapEntity<AILocatorSet> {

    @Override
    public void applyPropertyEdit(String propName, Property newValue) {
        switch (newValue) {
            case StringProperty sp when sp.name().equals("Name") -> MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, fileAddress, Util.getStringBytes(sp.value(), 16));
            default -> {}
        }
    }

    void addItem(EditorEntityProperty newItem) {
        MapWriter.addSpaceAtLocation(MapWriter.WritableObject.AI_LOCATOR, endAddress, 1);
        MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, endAddress, new byte[]{(byte) ((AILocator) newItem.value()).id()});
        MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, fileAddress + 16, Util.littleEndian(locators.size() + 1));
    }

    void removeItem(int idxToRemove) {
        int addr = fileAddress + 20 + idxToRemove;
        MapWriter.removeSpaceAtLocation(MapWriter.WritableObject.AI_LOCATOR, addr, 1);
        MapWriter.applyPatch(MapWriter.WritableObject.AI_LOCATOR, fileAddress + 16, Util.littleEndian(locators.size() - 1));
    }

    @Override
    public Map<String, Runnable> getButtonActions() {
        return Map.of("Delete", () -> AIWriter.removeLocatorSet(this));
    }

    @Override
    public String path() {
        return "AI/LocatorSets/"+name;
    }

    @Override
    public List<Property> properties() {
        List<Property> locatorProperties = locators.stream().map(l -> (Property) new EditorEntityProperty("dummy", l, false, false,"AI/Locators/")).toList();

        return List.of(
                new StringProperty("Name", name(), true, 16),
                new ListProperty("Locators", locatorProperties,true,
                        e -> OpenGG.asyncExec(() -> this.addItem((EditorEntityProperty) e)),
                        (e, i) -> OpenGG.asyncExec(() -> this.removeItem(i)),
                        "AI/Locators/")
        );
    }
}
