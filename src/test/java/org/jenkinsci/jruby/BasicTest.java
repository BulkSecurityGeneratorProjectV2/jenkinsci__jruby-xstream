package org.jenkinsci.jruby;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.embed.ScriptingContainer;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.proxy.InternalJavaProxy;
import org.jruby.javasupport.proxy.JavaProxyClass;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class BasicTest extends TestCase {
    private ScriptingContainer jruby;
    private XStream xs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        jruby = new ScriptingContainer();
        xs = new XStream() {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new JRubyMapper(next);
            }
        };
        JRubyXStream.register(xs, jruby.getProvider().getRuntime());
    }

    public void test1() {
        RubyObject o = (RubyObject)jruby.runScriptlet("require 'org/jenkinsci/jruby/basicTest'; o = Foo.new; o.bar = Bar.new; o.bar.x='test'; o.bar.y=5; o.bar.foo=Foo.new; o");
        String xml = xs.toXML(o);
        System.out.println(xml);
        Object r = xs.fromXML(xml);
    }

    public void testArray() {
        RubyArray before = (RubyArray)jruby.runScriptlet("[1,\"abc\",nil]");
        RubyArray after = roundtrip(before);

        assertEquals(before.length(), after.length());
        for (int i=0; i<before.getLength(); i++)
            assertEquals(before.entry(i), after.entry(i));
    }

    public void testHash() {
        RubyHash before = (RubyHash)jruby.runScriptlet("{ 1 => 5, \"foo\" => \"bar\", :abc => :def, \"d\" => [nil,nil]}");
        RubyHash after = roundtrip(before);

        assertTrue(before.op_equal(ThreadContext.newContext(jruby.getRuntime()),after).isTrue());
    }

    private <T> T roundtrip(T before) {
        String xml = xs.toXML(before);
        System.out.println(xml);
        return (T) xs.fromXML(xml);
    }

    public void testProxy() {
        Point before = (Point)jruby.runScriptlet("require 'org/jenkinsci/jruby/testProxy'; o=PointSubType.new; o.z=5; o");
        RubyClass c = ((InternalJavaProxy) before).___getInvocationHandler().getOrig().getMetaClass();

        Object after = roundtrip(new Object[]{before})[0];
        System.out.println(before);
        System.out.println(after);
    }
}
