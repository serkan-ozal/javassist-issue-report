package ozal.serkan.javassist.issue.report;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;

import java.io.InputStream;

/**
 * @author serkan
 */
public class RunnerInstrumentation {

    public static void main(String[] args) throws Exception {
        String classNameToInstrument = "ozal.serkan.javassist.issue.report.Runner$run$result$1";
        InputStream is =
                RunnerInstrumentation.class.getClassLoader().
                        getResourceAsStream(classNameToInstrument.replace(".", "/") + ".class");
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass(is);

        System.out.println("Instrumenting class " + ctClass.getName() + " ...");
        for (CtMethod ctMethod : ctClass.getMethods()) {
            if (Modifier.isNative(ctMethod.getModifiers()) || Modifier.isAbstract(ctMethod.getModifiers())) {
                continue;
            }
            ctMethod.insertBefore("System.out.println(\"before\");");
            ctMethod.insertAfter("System.out.println(\"after\");");
        }
        ctClass.debugWriteFile("./debug");
        Class clazz = ctClass.toClass();
        System.out.println("Instrumented class " + clazz.getName());

        Runner runner = new Runner();
        runner.run();
    }

}
