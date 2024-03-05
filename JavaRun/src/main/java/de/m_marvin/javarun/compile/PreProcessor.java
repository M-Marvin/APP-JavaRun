package de.m_marvin.javarun.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreProcessor {

	protected final static Pattern IMPORT_PATTERN = Pattern.compile("[\\t ]*import[\\t ]{1,}[A-Za-z0-9_\\.]{1,};");
	protected final static Pattern MAIN_METHOD_PATTERN = Pattern.compile("public static void main\\((String\\[\\]|String\\.\\.\\.) [A-Za-z0-9_]{1,}\\)");
	
	public class SkriptSource {
		
		public List<String> imports = new ArrayList<>();
		public String classCode;
		
	}
	
	public String process(String name, String scriptCode) {
		
		int mainMethodLine = findMainMethodRow(scriptCode);
		
		SkriptSource source = parseSource(scriptCode);
		addMainMethodIfMissing(source);
		
		if (mainMethodLine != -1) {
			offsetMainMethodTo(source, mainMethodLine);
		}
		
		return wrapInClass(name, source);
		
	}
	
	protected void offsetMainMethodTo(SkriptSource source, int mainMethodPosition) {
		
		int currentLine = findMainMethodRow(source.classCode) + source.imports.size() + 1;
		int offset = mainMethodPosition - currentLine;
		
		if (offset < 0) {
			String[] lines = source.classCode.split("\n");
			
			int linesRemovable = 0;
			while (linesRemovable < -offset) {
				if (!lines[linesRemovable].isBlank()) break;
				linesRemovable++;
			}
			
			StringBuilder sb = new StringBuilder();
			for (int i = linesRemovable; i < lines.length; i++) {
				sb.append(lines[i]).append("\n");
			}
			
			source.classCode = sb.toString();
		} else if (offset > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < offset; i++) {
				sb.append("\n");
			}
			sb.append(source.classCode);
			source.classCode = sb.toString();
		}
		
	}
	
	protected int findMainMethodRow(String code) {
		Matcher m = MAIN_METHOD_PATTERN.matcher(code);
		if (!m.find()) return -1;
		int index = m.start();
		return code.substring(0, index).split("\n").length;
	}
	
	protected SkriptSource parseSource(String scriptCode) {
		
		SkriptSource source = new SkriptSource();
		
		Matcher importMatcher = IMPORT_PATTERN.matcher(new String(scriptCode));
		
		while (importMatcher.find()) {
			
			source.imports.add(importMatcher.group());
			
		}
		
		source.classCode = importMatcher.replaceAll("");
		
		return source;
		
	}
	
	protected void addMainMethodIfMissing(SkriptSource source) {
		
		boolean hasMainMethod = MAIN_METHOD_PATTERN.matcher(source.classCode).find();
		
		if (hasMainMethod) return;
		
		source.classCode = "public static void main(String[] args) {\n" + source.classCode + "\n}";
		
	}
	
	protected String wrapInClass(String name, SkriptSource source) {
		
		StringBuilder sb = new StringBuilder();
		source.imports.forEach(imp -> sb.append(imp + "\n"));
		sb.append("public class " + name + " {");
		sb.append("\n" + source.classCode + "\n");
		sb.append("}");
		
		return sb.toString();
		
	}
	
}
