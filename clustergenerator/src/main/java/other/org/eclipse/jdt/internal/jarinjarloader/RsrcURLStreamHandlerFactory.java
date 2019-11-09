package other.org.eclipse.jdt.internal.jarinjarloader;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

//from https://www.cs.umb.edu/~smimarog/diclens/
//modified
public class RsrcURLStreamHandlerFactory implements URLStreamHandlerFactory {
	private final ClassLoader classLoader;
	private URLStreamHandlerFactory chainFac;

	public RsrcURLStreamHandlerFactory(final ClassLoader cl) {
		this.classLoader = cl;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("rsrc".equals(protocol)) {
			return new RsrcURLStreamHandler(this.classLoader);
		}
		if (this.chainFac != null) {
			return this.chainFac.createURLStreamHandler(protocol);
		}
		return null;
	}

	public void setURLStreamHandlerFactory(final URLStreamHandlerFactory fac) {
		this.chainFac = fac;
	}
}
