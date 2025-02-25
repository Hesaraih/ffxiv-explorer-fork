package com.fragmenterworks.ffxivextract.gui.modelviewer;

class ModelFurnitureEntry {

    public static final int TYPE_FURNITURE = 0;
    public static final int TYPE_YARDOBJECT = 1;

    final public int modelType;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final int id;
    final public String name;
    final public int model;
    final public String type;

    public ModelFurnitureEntry(int modelType, int id, String name, int model, String furnitureTypeName) {
        this.modelType = modelType;
        this.id = id;
        this.name = name;
        this.model = model;
        this.type = furnitureTypeName;
    }
}
