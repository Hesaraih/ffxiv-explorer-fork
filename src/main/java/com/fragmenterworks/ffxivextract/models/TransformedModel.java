package com.fragmenterworks.ffxivextract.models;

public class TransformedModel {
    public Vector3 Translation;
    public Vector3 Rotation;
    public Vector3 Scale;
    public Model Model;

    public TransformedModel(Model model, Vector3 translation, Vector3 rotation, Vector3 scale){
        this.Model = model;
        this.Translation = translation;
        this.Rotation = rotation;
        this.Scale = scale;
    }
}
