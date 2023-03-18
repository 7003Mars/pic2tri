package me.mars.triangles.ui;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.layout.Scl;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.pooling.Pools;
import me.mars.triangles.SchemBuilder;
import mindustry.Vars;
import mindustry.ui.Fonts;

import static me.mars.triangles.PicToTri.debugMode;

public class ImageGrid extends Element {
	private @Nullable TextureRegion region;
	int sx = 0, sy = 0;
	float panX = 0f, panY = 0f, zoom = 1f;

	ConverterDialog dialog;

	public ImageGrid(ConverterDialog dialog) {
		this.dialog = dialog;
		this.addListener(new DragListener(KeyCode.mouseRight) {
			@Override
			public void dragged(float xDelta, float yDelta) {
				float scaledSize = scaled(ImageGrid.this.zoom);
				xDelta/= scaledSize/2f;
				yDelta/= scaledSize/2f;
				ImageGrid.this.panX+= xDelta;
				ImageGrid.this.panY+= yDelta;
				ImageGrid.this.clampPos();
			}

			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
				ImageGrid.this.requestScroll();
				// REMOVEME
				ImageGrid.this.requestKeyboard();
			}

			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				ImageGrid.this.zoom = Mathf.clamp(ImageGrid.this.zoom - amountY/10f, 0.1f, 10f);
				ImageGrid.this.clampPos();
				return true;
			}
		});
		this.addListener(new ClickListener(){
			// REMOVEME
			@Override
			public boolean keyTyped(InputEvent event, char character) {
				switch (character) {
					case 'r' -> {
						ImageGrid.this.panX = ImageGrid.this.panY = 0f;
						return true;
					}
					case 'u' -> {
						ImageGrid.this.panY++;
						return true;
					}
					default -> {
						return false;
					}
				}
			}

			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (event.keyCode != KeyCode.mouseLeft) return;
				float scl = scaled(ImageGrid.this.zoom);
				x-= ImageGrid.this.width/2f;
				y-= ImageGrid.this.height/2f;
				x-= ImageGrid.this.panX * scl/2f;
				y-= ImageGrid.this.panY * scl/2f;
				// Remove the logic range padding
				int sx = (int) (x/scl - dialog.lBlock.range/Vars.tilesize);
				int sy = (int) (y/scl - dialog.lBlock.range/Vars.tilesize);
				sx/= dialog.lDisplay.size;
				sy/= dialog.lDisplay.size;
//				Log.info("select [@, @] at @, @", sx, sy);
				if (sx < 0 || sx >= dialog.xChunks || sy < 0 || sy >= dialog.yChunks) return;
				ImageGrid.this.sx = sx;
				ImageGrid.this.sy = sy;
				dialog.select(sx, sy);
			}
		});
	}

	public void setDrawable(@Nullable Pixmap pixmap) {
		if (this.region != null) {
			this.region.texture.dispose();
		}
		if (pixmap != null) {
			this.region = new TextureRegion(new Texture(pixmap));
		} else {
			this.region = null;
		}
	}

	@Override
	public void draw() {
		super.draw();
		Lines.rect(this.x, this.y, this.width, this.height);
		// Pre-drawing stuff
		if (this.region == null || !this.clipBegin()) return;
		float scaledSize = scaled(this.zoom);
		float ox = this.x+this.width/2f + this.panX*scaledSize/2f;
		float oy = this.y+this.height/2f + this.panY*scaledSize/2f;
		// pad = logic range * scaled size
		// ox <-> (scaledSize * builder width)

		Lines.rect(ox, oy, scaledSize * dialog.filler.width, scaledSize * dialog.filler.height);
		Fill.rect(this.panX + this.width/2f, this.panY + this.width/2f, scaledSize, scaledSize);
		// Drawing logic block positions
		for (SchemBuilder.Display display : dialog.filler.displays) {
			Draw.color(display.color);
			for (int i = 0; i < display.points.size; i++) {
				int pos = display.points.items[i];
				int x = Point2.x(pos);
				int y = Point2.y(pos);
				Fill.crect(ox + x*scaledSize, oy + y*scaledSize, scaledSize, scaledSize);
			}
		}
		Draw.color();
		// Draw the image
		int displayRes = (int) (dialog.lDisplay.size * scaledSize);
		int drawPad = (int) (dialog.lBlock.range / Vars.tilesize * scaledSize);
		float maxWidth = dialog.xChunks * displayRes;
		float maxHeight = dialog.yChunks * displayRes;
		float scl = Math.min(maxWidth / this.region.width, maxHeight / this.region.height);
		float iw = this.region.width * scl, ih = this.region.height * scl;
		Draw.rect(this.region, drawPad + ox + iw/2f, drawPad + oy + ih/2f, iw, ih);
		Lines.stroke(1f);
		for (int x = 0; x < dialog.xChunks; x++) {
			for (int y = 0; y < dialog.yChunks; y++) {
				Lines.rect(drawPad + ox + x*displayRes, drawPad + oy + y*displayRes, displayRes, displayRes);
			}
		}
		this.clipEnd();
		// Draw coords
		StringBuilder builder = new StringBuilder();
		builder.append((int) this.panX).append(",").append((int) this.panY);
		builder.append(" : ").append(Strings.fixed(this.zoom, 1));
		if (this.hasMouse()) {
			Vec2 mouse = this.screenToLocalCoordinates(Core.input.mouse());
			mouse.sub(this.width/2f, this.height/2f);
			if (debugMode) builder.append(" Raw: ").append((int) mouse.x).append(",").append((int) mouse.y);
			mouse.sub(this.panX*scaledSize/2f, this.panY*scaledSize/2f);
			// TODO: Figure out why this is already scaled down to block size
			mouse.scl(1f/(scaledSize));
			builder.append(" @ ").append((int) mouse.x).append(",").append((int) mouse.y);
		}
		Font font = Fonts.outline;
		font.setColor(Color.white);
		font.getData().setScale(Scl.scl(1f));
		GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
		layout.setText(font, builder.toString());
		Draw.color(0, 0, 0, 0.5f);
		Fill.rect(this.x + layout.width/2f, this.y + layout.height/2f, layout.width, layout.height);
		Draw.color();
		font.draw(layout, this.x, this.y + layout.height/2f);
		Pools.free(layout);
	}

	public void clampPos() {
		float scaledSize = scaled(this.zoom);
		float xbounds = (this.width) / (scaledSize);
		float ybounds = (this.height) / (scaledSize);
		float w = dialog.filler.width;
		float h = dialog.filler.height;
		this.panX = Mathf.clamp(this.panX, -xbounds-w, xbounds-w);
		this.panY = Mathf.clamp(this.panY, -ybounds-h, ybounds-h);
	}

	private static float scaled(float scl) {
		return scl * Vars.tilesize * 0.5f;
	}
}
