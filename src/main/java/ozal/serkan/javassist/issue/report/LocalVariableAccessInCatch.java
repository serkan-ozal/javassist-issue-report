package ozal.serkan.javassist.issue.report;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;
import javassist.compiler.CompileError;
import javassist.compiler.Javac;

public class LocalVariableAccessInCatch {

    public static void main(String[] args) throws Exception {
        ClassPool cp = ClassPool.getDefault();

        String helloWorldClassName = LocalVariableAccessInCatch.class.getName() + "$HelloWorld";
        String callContextClassName = LocalVariableAccessInCatch.class.getName() + "$CallContext";

        CtClass helloWorldClass = cp.get(helloWorldClassName);
        CtClass callContextClass = cp.get(callContextClassName);

        CtMethod sayHelloMethod = helloWorldClass.getDeclaredMethod("sayHello");
        sayHelloMethod.addLocalVariable("_callContext", callContextClass);
        int start = insertBefore(sayHelloMethod, "_callContext = new " + callContextClassName + "();");
        sayHelloMethod.insertAfter("_callContext.onFinish();", false);
        addCatch(sayHelloMethod, "_callContext.onError($e); throw $e;", cp.get(Throwable.class.getName()), start);

        // Define class with instrumented bytecode
        helloWorldClass.toClass();

        HelloWorld helloWorld = new HelloWorld();
        helloWorld.sayHello("Serkan");
    }

    private static int insertBefore(CtMethod method, String src) throws CannotCompileException {
        CtClass cc = method.getDeclaringClass();
        MethodInfo methodInfo = method.getMethodInfo();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null) {
            throw new CannotCompileException("no method body");
        }
        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(cc);
        try {
            int nvars = jv.recordParams(method.getParameterTypes(), Modifier.isStatic(method.getModifiers()));
            jv.recordParamNames(ca, nvars);
            jv.recordLocalVariables(ca, 0);
            CtClass returnType = Descriptor.getReturnType(methodInfo.getDescriptor(), cc.getClassPool());
            jv.recordReturnType(returnType, false);
            jv.compileStmnt(src);

            Bytecode b = jv.getBytecode();
            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack()) {
                ca.setMaxStack(stack);
            }
            if (locals > ca.getMaxLocals()) {
                ca.setMaxLocals(locals);
            }

            int pos = iterator.insertEx(b.get());
            iterator.insert(b.getExceptionTable(), pos);
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());

            return pos + b.getSize();
        } catch (NotFoundException e) {
            throw new CannotCompileException(e);
        } catch (CompileError e) {
            throw new CannotCompileException(e);
        } catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    private static void addCatch(CtMethod method, String src, CtClass exceptionType, int start) throws CannotCompileException {
        CtClass cc = method.getDeclaringClass();
        MethodInfo methodInfo = method.getMethodInfo();
        ConstPool cp = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(cp, ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(1);
        Javac jv = new Javac(b, cc);
        try {
            jv.recordParams(method.getParameterTypes(), Modifier.isStatic(method.getModifiers()));
            int var = jv.recordVariable(exceptionType, "$e");
            b.addAstore(var);
            jv.recordLocalVariables(ca, 0);
            jv.compileStmnt(src);

            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();
            if (stack > ca.getMaxStack()) {
                ca.setMaxStack(stack);
            }
            if (locals > ca.getMaxLocals()) {
                ca.setMaxLocals(locals);
            }

            int len = iterator.getCodeLength();
            int pos = iterator.append(b.get());

            ca.getExceptionTable().add(start, len, len, cp.addClassInfo(exceptionType));
            iterator.append(b.getExceptionTable(), pos);
            methodInfo.rebuildStackMapIf6(cc.getClassPool(), cc.getClassFile2());
        } catch (NotFoundException e) {
            throw new CannotCompileException(e);
        } catch (CompileError e) {
            throw new CannotCompileException(e);
        } catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    public static class HelloWorld {

        private static final int MAX_NAME_LENGTH = 3;

        public String sayHello(String name) {
            String logMessage = "Will say hello to " + name;
            System.out.println(logMessage);
            try {
                System.out.println("Working on it ...");
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            if (name.length() > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("Name length cannot be bigger than " + MAX_NAME_LENGTH);
            }
            return "Hello " + name;
        }

    }

    public static class CallContext {

        private final long startTime;

        public CallContext() {
            this.startTime = System.currentTimeMillis();
        }

        public void onFinish() {
            System.out.println(String.format(
                    "Completed with success in %d milliseconds",
                    (System.currentTimeMillis() - startTime)));
        }

        public void onError(Throwable error) {
            System.out.println(String.format(
                    "Completed with error (%s) in %d milliseconds",
                    error.getMessage(), (System.currentTimeMillis() - startTime)));
        }

    }

}
