package me.mars.triangles.layout;

import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;

public class ProcessorBuilder {
    int x, y;
    String code;
    Seq<LogicBlock.LogicLink> links = new Seq<>();

    private ProcessorBuilder(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public ProcessorBuilder(int x, int y, String code) {
        this(x, y);
        this.code = code;
    }


    public ProcessorBuilder(int x, int y, CodeBuilder code) {
        this(x, y);
        this.code = code.toString();
    }

    public void addAbsoluteLink(int x, int y, String name) {
        this.links.add(new LogicBlock.LogicLink(x, y, name, true));
    }

    public void addRelativeLink(int x, int y, String name) {
        this.links.add(new LogicBlock.LogicLink(this.x + x, this.y + y, name, true));
    }

    public Schematic.Stile getStile() {
        LogicBlock.LogicBuild proc = (LogicBlock.LogicBuild) Blocks.microProcessor.newBuilding();
        proc.tile = new Tile(x, y);
        proc.links.add(links);
        proc.updateCode(code);
        return new Schematic.Stile(Blocks.microProcessor, x, y, proc.config(),(byte) 0);
    }


}
