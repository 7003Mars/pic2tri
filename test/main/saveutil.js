function saveDisplay(x, y) {
    const display = Vars.world.build(x, y);
    const block = display.block;
    const size = block.displaySize;
    const w = size * (block == Blocks.logicDisplayTile ? block.maxDisplayDimensions : 1);
    const h = size * (block == Blocks.logicDisplayTile ? block.maxDisplayDimensions : 1);
    display.buffer.begin();
    ScreenUtils.saveScreenshot(new Fi("saved.png"), 0, 0, w, h);
    display.buffer.end();
}