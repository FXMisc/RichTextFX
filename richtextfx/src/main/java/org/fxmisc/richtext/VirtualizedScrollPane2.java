package org.fxmisc.richtext;

import javafx.beans.NamedArg;
import javafx.scene.Node;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;

/**
 * Created by Geoff on 4/14/2016.
 */
public class VirtualizedScrollPane2<TVirtualNode extends Node & Virtualized>
        extends VirtualizedScrollPane<TVirtualNode> {

    public VirtualizedScrollPane2(@NamedArg("content") TVirtualNode content) {
        super(content);
    }
}
