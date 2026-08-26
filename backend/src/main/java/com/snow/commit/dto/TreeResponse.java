package com.snow.commit.dto;

import com.snow.commit.entity.TreeStage;
import com.snow.commit.entity.TreeState;

public record TreeResponse(TreeState treeState, TreeStage treeStage, int currentStreak, long longestStreak) {
}
