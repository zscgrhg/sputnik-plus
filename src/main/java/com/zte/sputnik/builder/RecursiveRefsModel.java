package com.zte.sputnik.builder;

import com.zte.sputnik.parse.RefsInfo;
import com.zte.sputnik.trace.Invocation;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
public class RecursiveRefsModel {
    private Object id;
    private Invocation invocation;
    private RefsInfo refsInfo;
    private final List<RecursiveRefsModel> children = new ArrayList<>();
}
