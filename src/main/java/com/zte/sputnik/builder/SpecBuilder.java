package com.zte.sputnik.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.zte.sputnik.config.SputnikConfig;
import com.zte.sputnik.instrument.MethodNames;
import com.zte.sputnik.lbs.LoggerBuilder;
import com.zte.sputnik.parse.RefsInfo;
import com.zte.sputnik.parse.SubjectManager;
import com.zte.sputnik.trace.Invocation;
import com.zte.sputnik.trace.TraceReader;
import com.zte.sputnik.trace.TraceReaderImpl;
import com.zte.sputnik.util.MustacheUtil;
import com.zte.sputnik.util.RegexUtils;
import lombok.SneakyThrows;
import shade.sputnik.org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SpecBuilder {
    private static final Logger LOGGER = LoggerBuilder.of(SpecBuilder.class);
    public static final AtomicLong BUILD_INCR = new AtomicLong(1);
    public static final String FN = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    public static final String[] NAMES = IntStream.range((int) 'A', ((int) 'Z') + 1).mapToObj(x -> Character.toString((char) x)).toArray(String[]::new);
    private static TraceReader reader = new TraceReaderImpl();
    public static final Map<String,Boolean> SPEC_WRITER_BLOCK =Collections.synchronizedMap(new WeakHashMap<String,Boolean>());

    public static String argNameOf(int round) {

        StringBuilder builder = new StringBuilder("_var");
        while (round > 0) {
            if (round >= 16) {
                builder.append(NAMES[15]);
            } else {
                builder.append(NAMES[round - 1]);
            }
            round = round >> 4;
        }

        return builder.toString();
    }


    @SneakyThrows
    public static SpecModel build(Long subjectInvocationId, StageDescription description) {
        Invocation subjectInvocation = reader.readInvocation(subjectInvocationId);

        Class clazz = subjectInvocation.getClazzSource();
        String method = subjectInvocation.getMethod();
        SpecModel specModel = new SpecModel();
        specModel.pkg = clazz.getPackage().getName();
        specModel.subject = clazz.getSimpleName();
        specModel.id = subjectInvocation.id;
        specModel.method = method;
        String randomFileName=specModel.subject + FN;


        if (description != null) {
            SpecID specID = new SpecID();
            specID.subject = specModel.subject;
            specID.signature = specModel.signature;
            specID.fromClass = description.getTestClass();
            specID.fromMethod = description.getTestMethod();
            specID.displayName = description.getDisplayName();
            specID.uniqueId = description.getUniqueId();
            String hashcode = specID.toHashcode();
            specModel.className = specModel.subject + "S" + hashcode + "SpecTest";
            specModel.tc = description.getTestClass();
            specModel.tm = description.getTestMethod();
            specModel.title = description.getDisplayName();
        } else {
            specModel.className = randomFileName + "N" + BUILD_INCR.getAndIncrement() + "SpecTest";
        }

        specModel.signature = subjectInvocation.signature;
        specModel.namespace = specModel.pkg + "." + specModel.className;
        specModel.subjectDecl = MustacheUtil.format("def subject = new {{0}}()", clazz.getName());


        List<Invocation> children = subjectInvocation.children;

        Map<String, List<GroovyLine>> inputs = new HashMap<>();
        Map<String, List<GroovyLine>> outputs = new HashMap<>();
        Map<String, List<GroovyLine>> returned = new HashMap<>();
        Map<Long, Set<String>> staticInvokes = new HashMap<>();
        Map<Long, MethodNames> staticNames = new HashMap<>();
        inputs.put(String.valueOf(subjectInvocation.id), buildArgsLine(reader.readInParam(subjectInvocation.id)));
        JsonNode outParam = reader.readOutParam(subjectInvocation.id);
        outputs.put(String.valueOf(subjectInvocation.id), buildArgsLine(outParam));
        returned.put(String.valueOf(subjectInvocation.id), buildRetLine(outParam));

        if (!children.isEmpty()) {

            Map<RefsInfo, List<Invocation>> mapInv = new HashMap<>();
            for (Invocation child : children) {
                inputs.put(String.valueOf(child.id), buildArgsLine(reader.readInParam(child.id)));
                JsonNode childOutParam = reader.readOutParam(child.id);
                outputs.put(String.valueOf(child.id), buildArgsLine(childOutParam));
                returned.put(String.valueOf(child.id), buildRetLine(childOutParam));
                RefsInfo refPath = child.getRefsInfo();
                mapInv.putIfAbsent(refPath, new ArrayList<>());
                mapInv.get(refPath).add(child);
                if (child.staticInvoke
                        || RefsInfo.RefType.UNRESOLVABLE.equals(Optional
                        .ofNullable(child.refsInfo).map(RefsInfo::getType).orElse(null))) {
                    staticInvokes.putIfAbsent(child.mid, new HashSet<>());
                    staticInvokes.get(child.mid).add(buildMethodDef(MethodNames.METHOD_NAMES_MAP.get(child.mid)));
                    staticNames.putIfAbsent(child.mid, MethodNames.METHOD_NAMES_MAP.get(child.mid));
                }
            }

            final Map<Long, RecursiveRefsModel> rrm = buildRRM(mapInv);
            specModel.mockArgs = mapInv.keySet().stream().anyMatch(ref -> RefsInfo.RefType.ARG.equals(ref.type));

            List<String> mockBlock = mapInv.entrySet().stream()
                    .filter(e -> !RefsInfo.RefType.RETURNED.equals(e.getKey().type))
                    .flatMap(e ->
                            buildRecursiveMockBlock(specModel.namespace,
                                    e.getValue(), rrm, 1).stream()).collect(Collectors.toList());

            specModel.mockBlock = mockBlock;
            specModel.fieldsInitBlock = buildFieldsInitBlock(subjectInvocation);
        }
        specModel.Inputs = inputs.entrySet();
        specModel.Outputs = outputs.entrySet();
        specModel.Returned = returned.entrySet();
        specModel.actionDecl = buildWhen(subjectInvocation);
        specModel.assertDecl = buildAssert(subjectInvocation);
        specModel.staticInvokes = staticInvokes.entrySet();
        specModel.staticNames = staticNames.entrySet();
        return specModel;
    }

    private static List<String> buildFieldsInitBlock(Invocation subjectInvocation) {
        List<String> ret = new ArrayList<>();
        JsonNode values = reader.readValues(subjectInvocation.id);
        Iterator<String> names = values.fieldNames();
        while (names.hasNext()) {
            String next = names.next();
            JsonNode vmNode = values.get(next);
            JsonNode value = vmNode.get("value");
            if (value == null) {

            } else if (value.isTextual()) {
                ret.add(MustacheUtil.format("subject.{{0}}='''{{1}}'''", next, value.asText()));
            } else if (value.isValueNode()) {
                ret.add(MustacheUtil.format("subject.{{0}}={{1}}", next, value.asText()));
            } else {
                ret.add(MustacheUtil.format("subject.{{0}}=", next));
                String valueClass = vmNode.get("valueClass").asText();
                String declType = vmNode.get("declType").asText();
                List<GroovyLine> groovyLines = jsonToGroovyMap(1, null, value, declType, valueClass);
                endBlock(groovyLines, null);
                List<String> codes = groovyLines.stream()
                        .map(gl -> MustacheUtil.formatModel("{{ident}}{{tokens}}{{lineEnd}}", gl))
                        .collect(Collectors.toList());
                ret.addAll(codes);

            }
        }
        return ret;
    }

    private static String buildMethodDef(MethodNames methodNames) {
        String name = methodNames.method.getReturnType().getName();
        String signature = methodNames.signature;
        AtomicInteger atomicInteger = new AtomicInteger(0);
        int length = methodNames.method.getParameterTypes().length;
        String defLine = signature;
        if (length > 0) {
            defLine = RegexUtils.replaceAll(signature, "(?=\\s*[,)])",
                    matcher -> " arg" + atomicInteger.getAndIncrement());
        }

        return name + " " + defLine;
    }

    private static Map<Long, RecursiveRefsModel> buildRRM(Map<RefsInfo, List<Invocation>> mapInv) {
        Map<Long, RecursiveRefsModel> rrm = new HashMap<>();
        for (Map.Entry<RefsInfo, List<Invocation>> entry : mapInv.entrySet()) {
            for (Invocation invocation : entry.getValue()) {
                RecursiveRefsModel rm = new RecursiveRefsModel();
                rm.setInvocation(invocation);
                rm.setRefsInfo(entry.getKey());
                rrm.put(invocation.id, rm);
            }
        }
        for (RecursiveRefsModel value : rrm.values()) {
            RefsInfo refsInfo = value.getRefsInfo();
            if (refsInfo != null && RefsInfo.RefType.RETURNED.equals(refsInfo.type)) {
                RecursiveRefsModel parent = rrm.get(refsInfo.returnedFrom);
                parent.getChildren().add(value);
            }
        }
        return Collections.unmodifiableMap(rrm);
    }

    public static String buildWhen(Invocation invocation) {
        Class[] argsType = invocation.argsType;
        List<String> argString = new ArrayList<>();
        for (int i = 0; i < argsType.length; i++) {
            String sp = (i < argsType.length - 1) ? "," : "";
            if (RefsInfo.class.isAssignableFrom(argsType[i])) {
                RefsInfo refsInfo = invocation.argsNames.get(i);
                if (RefsInfo.RefType.FIELD.equals(refsInfo.type)) {
                    argString.add(MustacheUtil.format("subject.{{0}}{{1}}", refsInfo.name, sp));
                } else {
                    argString.add(MustacheUtil.format("argMockDefs.{{0}}{{1}}", refsInfo.name, sp));
                }

            } else {
                argString.add(MustacheUtil.format("INPUTS{{0}}[{{1}}]{{2}}", invocation.id, i, sp));
            }
        }

        String action = MustacheUtil.format("def ret=subject.{{0}}({{#1}}{{.}}{{/1}})", invocation.method, argString);
        return action;
    }

    public static String buildAssert(Invocation invocation) {


        return MustacheUtil.format("ret == RETURNED{{0}}||ret.propertyMatches(RETURNED{{0}})", invocation.id);
    }

    public static List<String> buildRecursiveMockBlock(String namespace, List<Invocation> invocationList, Map<Long, RecursiveRefsModel> rrm, int varCounter) {
        if (invocationList == null || invocationList.isEmpty()) {
            return Collections.emptyList();
        }
        assert invocationList.stream().map(Invocation::getRefsInfo).distinct().count() == 1;

        List<String> ret = new ArrayList<>();
        Invocation invo = invocationList.get(0);
        MethodNames names = MethodNames.METHOD_NAMES_MAP.get(invo.mid);
        RefsInfo refsInfo = invo.getRefsInfo();
        Class declaredType = refsInfo.declaredType;
        boolean opened = false;
        if (RefsInfo.RefType.FIELD.equals(refsInfo.type)) {
            opened = true;
            ret.add(MustacheUtil.format("subject.{{0}}=Mock({{1}}){", refsInfo.name, declaredType.getName()));
        } else if (RefsInfo.RefType.ARG.equals(refsInfo.type)) {
            opened = true;
            ret.add(MustacheUtil.format("argMockDefs.{{0}}=Mock({{1}}){", refsInfo.name, declaredType.getName()));
        } else if (RefsInfo.RefType.INVOKE_STATIC.equals(refsInfo.type)
                || RefsInfo.RefType.UNRESOLVABLE.equals(refsInfo.type)) {
            opened = true;
            ret.add(MustacheUtil.format("invokeStaticDefs['{{0}}::{{1}}@{{2}}']=Mock(StaticStub{{3}}){",
                    names.ownerName, names.erased, namespace, invo.mid));
        } else if (RefsInfo.RefType.RETURNED.equals(refsInfo.type)) {

        } else {
            throw new IllegalStateException("unknown  RefType");
        }

        for (Invocation invocation : invocationList) {
            String untypePrefix = argNameOf(varCounter++);
            String argNamePrefix = argNameOf(varCounter++);
            String closureInputArgDef = argNameOf(varCounter++);

            String argTypeNames = invocation.getSignature().replaceAll("^.*\\((.*?)\\)", "$1");
            String[] argTypeNamesSplit = Stream.of(argTypeNames.split(",")).filter(s -> !s.trim().isEmpty()).toArray(String[]::new);
            int length = argTypeNamesSplit.length;
            String argMatcherLine = IntStream.range(0, length).mapToObj(i -> "{"
                    + untypePrefix + i + "-> "
                    + untypePrefix + i + "==(INPUTS{{1}}[" + i + "])||"
                    + untypePrefix + i + ".propertyMatches(INPUTS{{1}}[" + i + "])}")
                    .collect(Collectors.joining(","));
            //String argsLine = IntStream.range(0, length).mapToObj(i -> "INPUTS{{1}}[" + i + "]").collect(Collectors.joining(","));
            String closureArgDefLine = IntStream.range(0, length)
                    .mapToObj(i -> argTypeNamesSplit[i] + " " + argNamePrefix + i + "")
                    .collect(Collectors.joining(";"));
            String closureArgAssignLine = IntStream.range(0, length)
                    .mapToObj(i -> argNamePrefix + i + "")
                    .collect(Collectors.joining(","));
            ret.add(MustacheUtil.format("1 * {{0}}(" + argMatcherLine + ") >> { \n\t" + closureInputArgDef + "->",
                    invocation.method, invocation.id));
            if (!closureArgDefLine.isEmpty()) {
                List<String> copyLine = IntStream.range(0, length)
                        .boxed()
                        .flatMap(i -> {
                            String left = argNamePrefix + i + "";
                            String right = MustacheUtil.format("OUTPUTS{{0}}[" + i + "]", invocation.id);
                            String copy = MustacheUtil.format("({{1}}=={{0}})||{{1}}.copyDirty({{0}})", left, right);//
                            return Stream.of(copy);
                        }).collect(Collectors.toList());
                ret.add(closureArgDefLine);
                ret.add("(" + closureArgAssignLine + ")=" + closureInputArgDef);
                ret.addAll(copyLine);
            }


            //ret.add(MustacheUtil.format("1 * {{0}}(" + argsLine + ") >> RETURNED{{1}} ", invocation.method, invocation.id));

            //ret.add(MustacheUtil.format("return RETURNED{{0}} ", invocation.id));
            List<RecursiveRefsModel> children = rrm.get(invocation.id).getChildren();
            if (!SubjectManager.isTraced(invocation.returnedType)) {
                ret.add(MustacheUtil.format("return RETURNED{{0}} ", invocation.id));
            } else {
                ret.add(MustacheUtil.format("return Mock({{0}}){", invocation.returnedType.getName()));
                List<Invocation> collect = children.stream().map(RecursiveRefsModel::getInvocation).collect(Collectors.toList());
                ret.addAll(buildRecursiveMockBlock(namespace, collect, rrm, varCounter+16));
                ret.add("}");
            }
            ret.add("}");
        }
        if (opened) {
            ret.add("}");
        }
        return ret;
    }

    public static List<GroovyLine> buildRetLine(JsonNode paramModel) {
        List<GroovyLine> ret = new ArrayList<>();
        JsonNode returned = paramModel.get("returned");
        JsonNode rgt = paramModel.get("returnedGenericType");
        ret.addAll(jsonToGroovyMap(1, null, returned, rgt.asText(), null));
        endBlock(ret, null);
        return ret;
    }

    public static List<GroovyLine> buildArgsLine(JsonNode paramModel) {
        List<GroovyLine> ret = new ArrayList<>();
        JsonNode args = paramModel.get("args");
        ArrayNode argValues = (ArrayNode) args;
        JsonNode at = paramModel.get("argsType");
        ArrayNode atArr = (ArrayNode) at;

        JsonNode agt = paramModel.get("argsGenericType");
        ArrayNode agtArr = (ArrayNode) agt;
        if (argValues != null && argValues.size() > 0) {
            for (int i = 0; i < argValues.size(); i++) {
                ret.addAll(jsonToGroovyMap(1, null, argValues.get(i), agtArr.get(i).asText(), atArr.get(i).asText()));
            }
        }
        endBlock(ret, null);
        return ret;
    }

    private static List<GroovyLine> jsonToGroovyMap(int ident, String name, JsonNode value) {
        List<GroovyLine> lines = jsonToGroovyMap(ident, name, value, null, null);
        return lines;
    }

    private static void endBlock(List<GroovyLine> lines, String endChar) {
        if (lines != null && !lines.isEmpty()) {
            GroovyLine groovyLine = lines.get(lines.size() - 1);
            groovyLine.setLineEnd(endChar);
        }
    }

    @SneakyThrows
    public static List<GroovyLine> jsonToGroovyMap(int ident, String name, JsonNode value, String genericSignature, String valueType) {
        ident++;
        String identStr = IntStream.range(0, ident).mapToObj(i -> "\t").collect(Collectors.joining());
        List<GroovyLine> defs = new ArrayList<>();

        if (value.isArray()) {
            defs.add(new GroovyLine(identStr, MustacheUtil.format("{{#0}}{{0}}:{{/0}}[", groovyId(name)), null));
            ArrayNode arrayNode = (ArrayNode) value;
            List<GroovyLine> subLines = new ArrayList<>();
            for (int i = 0; i < arrayNode.size(); i++) {
                subLines.addAll(jsonToGroovyMap(ident, null, arrayNode.get(i)));
            }
            endBlock(subLines, null);
            defs.addAll(subLines);
            defs.add(new GroovyLine(identStr, "]"));
        } else if (value == null) {
            defs.add(new GroovyLine(identStr, null));
        } else if (value.isTextual()) {
            defs.add(new GroovyLine(identStr, MustacheUtil.format("{{#0}}{{0}}:{{/0}}'''{{1}}'''", groovyId(name), value.asText())));
        } else if (value.isValueNode()) {
            if (void.class.getName().equals(genericSignature)
                    || Void.class.getName().equals(genericSignature)) {
                defs.add(new GroovyLine(identStr, MustacheUtil.format("{{#0}}{{0}}:{{/0}}{{1}}", groovyId(name), value.asText())));
            } else {
                defs.add(new GroovyLine(identStr, MustacheUtil.format("{{#0}}{{0}}:{{/0}}{{1}} {{#2}}as {{2}}{{/2}}", groovyId(name), value.asText(), genericSignature)));
            }

        } else {
            assert value.isObject();
            defs.add(new GroovyLine(identStr, MustacheUtil.format("{{#0}}{{0}}:{{/0}}[", groovyId(name)), null));
            Iterator<String> names = value.fieldNames();
            List<GroovyLine> subLines = new ArrayList<>();
            while (names.hasNext()) {
                String nextName = names.next();
                JsonNode subNode = value.get(nextName);
                subLines.addAll(jsonToGroovyMap(ident, nextName, subNode));
            }
            endBlock(subLines, null);
            defs.addAll(subLines);
            defs.add(new GroovyLine(identStr, "]"));
        }
        if (defs.size() > 1 && genericSignature != null && !genericSignature.isEmpty()) {
            GroovyLine groovyLine = defs.get(defs.size() - 1);
            if (RefsInfo.class.getName().equals(valueType)) {
                groovyLine.tokens = MustacheUtil.format("{{0}} as {{1}}", groovyLine.tokens, RefsInfo.class.getName());
            } else if (genericSignature.contains("<")) {
                groovyLine.tokens = MustacheUtil.format("{{0}}.reconstruction(new TypeReference<{{1}}>(){})", groovyLine.tokens, genericSignature);
            } else if (void.class.getName().equals(genericSignature)
                    || Void.class.getName().equals(genericSignature)) {
                groovyLine.tokens = MustacheUtil.format("{{0}}", groovyLine.tokens);
            } else if (genericSignature.contains(".")
                    && !genericSignature.startsWith("java.lang.")) {
                groovyLine.tokens = MustacheUtil.format("{{0}}.reconstruction(new TypeReference<{{1}}>(){})", groovyLine.tokens, genericSignature);
            } else {
                groovyLine.tokens = MustacheUtil.format("{{0}} as {{1}}",
                        groovyLine.tokens,
                        Optional.ofNullable(valueType)
                                .map(SpecBuilder::normalizeArrayTypeName)
                                .orElse(genericSignature));
            }

        }

        return defs;
    }

    public static String normalizeArrayTypeName(String typeName) {
        if (!typeName.startsWith("[")) {
            return typeName;
        }
        int i = typeName.lastIndexOf("[");
        StringBuilder sb = new StringBuilder();
        switch (typeName.charAt(i + 1)) {
            case 'B':
                sb.append("byte");
                break;
            case 'C':
                sb.append("char");
                break;
            case 'S':
                sb.append("short");
                break;
            case 'I':
                sb.append("int");
                break;
            case 'J':
                sb.append("long");
                break;
            case 'F':
                sb.append("float");
                break;
            case 'D':
                sb.append("double");
                break;
            case 'Z':
                sb.append("boolean");
                break;
            case 'L':
                sb.append(typeName.substring(i + 2).replaceAll(";+$", ""));
                break;
            default:
                throw new IllegalStateException();
        }
        for (int j = 0; j <= i; j++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    public static String groovyId(String name) {
        if (name == null || javax.lang.model.SourceVersion.isIdentifier(name)) {
            return name;
        }
        final char singleQuote = (char) 39;
        final char backslash = (char) 92;
        StringBuilder builder = new StringBuilder();
        builder.append(singleQuote);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == singleQuote) {
                builder.append(backslash);
            }
            builder.append(c);
        }
        builder.append(singleQuote);
        return builder.toString();
    }


    @SneakyThrows
    public static void writeSpec(Long subjectInvocationId, StageDescription description) {
        SpecModel model = build(subjectInvocationId, description);

        if (SPEC_WRITER_BLOCK.putIfAbsent(model.className, true) != null) {
            return;
        }
        model.invocationId = subjectInvocationId;

        Path pkg = SputnikConfig.INSTANCE
                .getSpecOutputsDir()
                .toPath()
                .resolve(model.getPkg());
        File pkgDir = pkg.toFile();
        if (!pkgDir.exists()) {
            pkgDir.mkdirs();
        }
        String specText = MustacheUtil.render("btm/spec.mustache", model);
        Path resolve = pkg.resolve(model.className + ".groovy");
        LOGGER.debug("write :" + resolve.toString());
        Files.write(resolve,
                specText.getBytes("UTF-8"),
                StandardOpenOption.CREATE_NEW);

        Invocation invocation = reader.readInvocation(subjectInvocationId);
        List<Invocation> children = invocation.children;
        for (Invocation child : children) {
            if (child.subject) {
                writeSpec(child.id, description);
            }
        }

    }
}
