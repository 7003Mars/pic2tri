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

function findProc(x1, y1, x2, y2, check) {
    for (let x = x1; x < x2; x++) {
        for (let y = y1; y < y2; y++) {
            const build = Vars.world.build(x, y);
            const block = build.block;
            if (block != Blocks.microProcessor) continue;
            const res = check(build);
            if (res != null) {
                Log.info("Match at (x, y): @", x, y, res)
            }
        }
    }
}

function grepProc(x1, y1, x2, y2, substring) {
    findProc(x1, y1, x2, y2, (build) => {
        const code = build.code;
        for (line of code) {
            if (line.includes(substring)) {
                return line;
            }
        }
    })
}

function resetCounters(x1, y1, x2, y2) {
    for (let x = x1; x < x2; x++) {
        for (let y = y1; y < y2; y++) {
            const build = Vars.world.build(x, y);
            const block = build.block;
            if (block != Blocks.microProcessor) continue;
            build.executor.counter.setNum(0);
        }
    }
}