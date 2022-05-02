package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class LineColAnnotator extends PreorderJmmVisitor<Integer, Integer> {
    LineColAnnotator() {
        setDefaultVisit(this::annotate);
    }

    private Integer annotate(JmmNode node, Integer dummy) {
        BaseNode baseNode = (BaseNode) node;

        node.put("line", Integer.toString(baseNode.getBeginLine()));
        node.put("col", Integer.toString(baseNode.getBeginColumn()));

        return 0;
    }
}
