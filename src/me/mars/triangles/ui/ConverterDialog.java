package me.mars.triangles.ui;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.*;
import me.mars.triangles.Converter;
import me.mars.triangles.SchemBuilder;
import me.mars.triangles.Generator;
import mindustry.Vars;

import static me.mars.triangles.PicToTri.internalName;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.ui;
import static me.mars.triangles.PicToTri.bundle;

import mindustry.content.Blocks;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicDisplay;

public class ConverterDialog extends BaseDialog {
	public LogicDisplay lDisplay = (LogicDisplay) Blocks.largeLogicDisplay;
	public LogicBlock lBlock = (LogicBlock) Blocks.microProcessor;
	private @Nullable Pixmap currentLoaded;
	Fi lastSelectedFile;

	Options options;
	Configs configs;
	Table leftTab;
	ImageGrid image;

	SchemBuilder filler = new SchemBuilder(0, 0, 0, 0);
	int xChunks = 0, yChunks = 0;
	public float scl = 1f;

	@Nullable
	Generator.GenOpts selectedOpt = null;
	Seq<Converter> converters = new Seq<>();

	public ConverterDialog() {
		super("Image converter");
		this.closeOnBack();
		this.addCloseButton ();
		// Buttons
		this.buttons.button(bundle("select"), () -> Vars.platform.showFileChooser(true, "png", (fi) -> {
			try {
				if (this.currentLoaded != null) this.currentLoaded.dispose();
				this.lastSelectedFile = fi;
				this.loadPixmap(fi);
			} catch (ArcRuntimeException e) {
				ui.showErrorMessage(bundle("load-fail"));
			}
		}));
		this.buttons.button(bundle("submit"), () -> {
			if (this.currentLoaded == null) {
				ui.showInfoToast(bundle("submit-empty"), 5f);
				return;
			}
			// TODO: The seed parsing should never fail
			this.converters.add(new Converter(this.currentLoaded, (int) (this.currentLoaded.width * this.scl),
					(int) (this.currentLoaded.height * this.scl), this.filler, this.lBlock, this.lDisplay,
					Strings.parseInt(this.configs.seedField.getText(), 0), this.configs.nameField.getText())
					.submit(this.filler.displays.<Generator.GenOpts>as()));
			this.convertersUpdated();
			this.filler = new SchemBuilder(0, 0, 0, 0);
			this.xChunks = this.yChunks = 0;
			this.image.setDrawable(null);
			this.currentLoaded.dispose();
			this.currentLoaded = null;
		});
		this.cont.center();
		// Tables
		this.configs = new Configs(this);
		this.options = new Options(this);
		this.cont.pane(this.leftTab = new Table()).width(275).growY().fill();
		this.leftTab.defaults().left();
		this.cont.add(this.image = new ImageGrid(this)).grow();
		Table rightTab = new Table();
		rightTab.add(options);
		rightTab.row();
		rightTab.add(configs);
		this.cont.add(rightTab).width(200).growY().fill();
	}

	public void loadPixmap(Fi fi) throws ArcRuntimeException {
		Pixmap pixmap = new Pixmap(fi);


		if (this.currentLoaded != null) this.currentLoaded.dispose();
		int minSize = this.lDisplay.displaySize;
		if (pixmap.width < minSize && pixmap.height < minSize) {
			float scale = Math.max((float) minSize / pixmap.width, (float) minSize / pixmap.height);
			Vars.ui.showInfoToast(Core.bundle.format(internalName+".too-small", scale), 5f);
			Pixmap old = pixmap;
			pixmap = Pixmaps.scale(old, scale);
			old.dispose();
		}
		this.currentLoaded = pixmap;
		this.image.setDrawable(pixmap);
		this.updateScales();
		// Set config stuff
		this.configs.nameField.setText(fi.nameWithoutExtension());
		String seed = Core.settings.getString(internalName+".default-seed");
		this.configs.seedField.setText(seed.isEmpty() ? String.valueOf(Mathf.random(Integer.MAX_VALUE-1)) : seed);
		Log.debug("range of @-@", this.minScl(), this.maxScl());
	}

	public void updateScales() {
		if (this.currentLoaded == null) return;
		// Note: This indirectly calls updateSize too
		Seq<Float> snaps = new Seq<>();
		int snapSize = this.lDisplay.displaySize;
		while (snapSize <= this.maxSize()) {
			snaps.add((float) snapSize / this.currentLoaded.width);
			snaps.add((float) snapSize / this.currentLoaded.height);
			snapSize+= this.lDisplay.displaySize;
		}
		snaps.sort(f -> f);
		this.xChunks = this.yChunks = 0;
		this.configs.scaleSlider.setValues(snaps);
	}

	public void updateSize() {
		if (this.currentLoaded == null) return;
		int prevX = this.xChunks, prevY = this.yChunks;
		int displayRes = this.lDisplay.displaySize;
		this.xChunks = Mathf.ceilPositive((int)(this.currentLoaded.width*this.scl) / (float)displayRes);
		this.yChunks = Mathf.ceilPositive((int)(this.currentLoaded.height*this.scl) / (float) displayRes);
		if (prevX != this.xChunks || prevY != this.yChunks) {
			this.filler = new SchemBuilder(this.xChunks, this.yChunks, (int) (this.lBlock.range/tilesize), this.lDisplay.size);
		}
	}

	public void convertersUpdated() {
		this.leftTab.clearChildren();
		for (Converter conv : this.converters) {
			this.leftTab.add(new ConverterWrapper(this, conv));
			this.leftTab.row();
		}
	}

	public double minScl() {
		int minSize = this.lDisplay.displaySize;
		return Math.max((float) minSize / this.currentLoaded.height, (float) minSize / this.currentLoaded.width);
	}

	public int maxSize() {
		return Mathf.ceilPositive(this.lBlock.range/tilesize / this.lDisplay.size)*2 * this.lDisplay.displaySize;
	}

	public double maxScl() {
		return Math.max((double) this.maxSize() / this.currentLoaded.height,
				(double) this.maxSize() / this.currentLoaded.width);
	}

	public void select(int cx, int cy) {
		if (cx < 0 || cx >= this.xChunks || cy < 0 || cy >= this.yChunks) {
			Log.err("@, @ not withing range 0-@, 0-@", cx, cy, this.xChunks, this.yChunks);
			return;
		}
		this.selectedOpt = this.filler.getDisplay(cx, cy);
		this.options.updateFields();
		// TODO: Update config
	}

}
