package de.m_marvin.javarun;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.stream.Stream;

import de.m_marvin.javarun.compile.MemoryClassLoader;
import de.m_marvin.javarun.compile.PreProcessor;
import de.m_marvin.javarun.compile.SourceCompiler;

public class JavaRun {
	
	public static void main(String... args) {
		
		if (args.length >= 1) {
			
			File skriptFile = new File(args[0]);
			
			String classpath = ".";
			String[] arguments;
			if (args.length > 2 && args[1].equals("-classpath")) {
				classpath = args[2];
				arguments = Arrays.copyOfRange(args, 3, args.length);
			} else {
				arguments = Arrays.copyOfRange(args, 1, args.length);
			}
			
			if (skriptFile.isFile()) {
				boolean result = runSkript(skriptFile, classpath, arguments);
				System.exit(result ? 0 : -1);;
			}
			
		}
		
		System.out.println("javarun [file] (-classpath [paths1;path2 ...]) (arguments)...");
		
	}
	
	public static final PreProcessor PREPROCESSOR = new PreProcessor();
	public static final SourceCompiler SOURCE_COMPILER = new SourceCompiler();
	public static final MemoryClassLoader CLASS_LOADER = new MemoryClassLoader(SOURCE_COMPILER.getClassFileManager());
	public static final String MAIN_SCRIPT_CLASS = "SkriptMain";
	
	public static boolean runSkript(File skriptFile, String classpath, String... arguments) {
		
		if (!skriptFile.isFile()) {
			System.err.println("Skript file does not exist!");
			return false;
		}
		
		try {
			InputStream input = new FileInputStream(skriptFile);
			String skript = new String(input.readAllBytes());
			input.close();
			
			return runSkript(skript, classpath, arguments);
			
		} catch (Exception e) {
			System.out.println("Could not load skript file");
			e.printStackTrace();
			return false;
		}
		
	}
	
	public static boolean runSkript(String skript, String classpath, String... arguments) {
		
		String classCode = PREPROCESSOR.process(MAIN_SCRIPT_CLASS, skript);
		
		if (!SOURCE_COMPILER.compile(MAIN_SCRIPT_CLASS, classCode, classpath)) {
			System.err.println("Compilation failed!");
			return false;
		}
		
		try {
			
			URL[] classpathURLs = Stream.of(classpath.split(";"))
					.map(path -> {
						try {
							return new File(path).toURI().toURL();
						} catch (Exception e) {
							e.printStackTrace();
							return null;
						}
					})
					.toArray(i -> new URL[i]);
			
			CLASS_LOADER.setParentLoader(URLClassLoader.newInstance(classpathURLs, Thread.currentThread().getContextClassLoader()));
			Class<?> mainSkriptClass = CLASS_LOADER.loadClass(MAIN_SCRIPT_CLASS);
			
			Method mainMethod = mainSkriptClass.getDeclaredMethod("main", String[].class);
			
			mainMethod.invoke(null, (Object) arguments);
			return true;
			
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to load skript after compillation!");
			e.printStackTrace();
			return false;
		} catch (NoSuchMethodException e) {
			System.err.println("Failed to find main method!");
			e.printStackTrace();
			return false;
		} catch (SecurityException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		} catch (InvocationTargetException e) {
			System.err.println("Script threw an error: " + e.getCause().getMessage());
			e.getCause().printStackTrace();
			return false;
		}
		
	}
	
}
