package de.m_marvin.javarun.compile;

import de.m_marvin.javarun.compile.SourceCompiler.InMemoryFileManager;

public class MemoryClassLoader extends ClassLoader {
	
	protected InMemoryFileManager manager;
	protected ClassLoader parentLoader;
	
	public MemoryClassLoader(InMemoryFileManager manager) {
		this.manager = manager;
	}
	
	public void setParentLoader(ClassLoader parentLoader) {
		this.parentLoader = parentLoader;
	}
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytes = this.manager.getClassBytes(name);
		if (bytes == null) {
			if (this.parentLoader == null) throw new ClassNotFoundException("InMemory class '" + name + "' not found!");
			return this.parentLoader.loadClass(name);
		}
		return defineClass(name, bytes, 0, bytes.length);
	}
	
}
