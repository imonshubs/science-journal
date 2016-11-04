/*
 *  Copyright 2016 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.google.android.apps.forscience.whistlepunk.devicemanager;

import android.support.annotation.NonNull;

import com.bignerdranch.expandablerecyclerview.Adapter.ExpandableRecyclerAdapter;
import com.bignerdranch.expandablerecyclerview.Model.ParentListItem;
import com.bignerdranch.expandablerecyclerview.ViewHolder.ChildViewHolder;
import com.google.common.base.Supplier;

import java.util.List;

public abstract class CompositeSensitiveExpandableAdapter<PVH extends OffsetParentViewHolder, CVH
        extends ChildViewHolder> extends ExpandableRecyclerAdapter<PVH, CVH> implements
        CompositeRecyclerAdapter.CompositeSensitiveAdapter {
    private int mGlobalAdapterStartPosition = 0;

    public CompositeSensitiveExpandableAdapter(
            @NonNull List<? extends ParentListItem> parentItemList) {
        super(parentItemList);
    }

    protected Supplier<Integer> offsetSupplier() {
        return new Supplier<Integer>() {
            @Override
            public Integer get() {
                return mGlobalAdapterStartPosition;
            }
        };
    }

    @Override
    public void informGlobalAdapterStartPosition(int startPosition) {
        mGlobalAdapterStartPosition = startPosition;
    }
}