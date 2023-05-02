package me.mars.triangles;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Threads;
import me.mars.triangles.shapes.Shape;

import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Goof {
	static ExecutorService executor = Threads.executor("bruh", 2);
	public static void launch() {
		Fi gifDir = Core.settings.getDataDirectory().child("gif");
		Fi outDir = Core.settings.getDataDirectory().child("bad-apple-out");
		if (!gifDir.exists()) {
			Log.err("Couldn't find gif folder in data dir");
			return;
		}
		Seq<Fi> processFiles = gifDir.findAll(f -> f.extEquals("png")).sort(Comparator.comparing(Fi::name));
		Log.info("Processing in order:\n @", processFiles);
		Fi first = processFiles.first();
		Seq<Future<Seq<Shape>>> tasks = new Seq<>();
		Seq<Seq<Shape>> shapes = new Seq<>();
		MutateMap m1 = new MutateMap(new Pixmap(first));
		MutateMap m2 = new MutateMap(new Pixmap(first));
		for (Fi file : processFiles) {
			tasks.clear();
			shapes.clear();
			Generator gen1 = new Generator(file, m1, false);
			Generator gen2 = new Generator(file, m2, true);
			tasks.add(executor.submit(gen1));
			tasks.add(executor.submit(gen2));
			shapes.addAll(tasks.map(f -> {
				try {
					return f.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}));
			Seq<Shape> best;
			Log.info("@ finished (@, @)", file.nameWithoutExtension(), gen1.acc(), gen2.acc());
			float acc = gen2.acc();
			if (acc > gen1.acc() || acc > 0.98) {
				best = shapes.get(1);
				m1.draw(m2);
			} else {
				best = shapes.get(0);
			}
			StringBuilder sb = new StringBuilder();
			best.each(shape -> sb.append(shape.toInstr()));
			String name = file.nameWithoutExtension() + "-out.txt";
			outDir.child(name).writeString(sb.toString());
		}

	}
}
