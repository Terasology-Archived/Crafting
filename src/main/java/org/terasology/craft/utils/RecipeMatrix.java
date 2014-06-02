/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.craft.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by adeon on 02.06.14.
 */
public class RecipeMatrix {
    public int width = 3;
    public int height = 3;
    public Map<String, List<String>> recipe = null;
    public static final String EMPTY_ROW = " ";

    public RecipeMatrix(Map<String, List<String>> recipe) {
        this(recipe, 3, 3);
    }

    public RecipeMatrix(Map<String, List<String>> recipe, int width, int height) {
        this.recipe = recipe;
        this.width = width;
        this.height = height;
    }

    /*
     * Deleted empty columns and rows
     *
     * For example, we have some matrix like this:
     *
     * 1 0 1
     * 1 0 1
     * 0 0 0
     *
     */
    public RecipeMatrix trim() {
        HashMap<String, List<String>> matrix = new HashMap<String, List<String>>();

        ArrayList<Integer> counterLines = new ArrayList<Integer>(width);
        ArrayList<Integer> counterColumns = new ArrayList<Integer>(height);
        int countLevels = recipe.size();

        //calculate count for empty rows
        for (int i = 0; i < height; i++) {
            if (counterLines.size() < (i + 1)) {
                counterLines.add(0);
            }
            for (int j = 0; j < width; j++) {
                for (List<String> currentLevel : recipe.values()) {
                    if (currentLevel.get(i * width + j).equals(EMPTY_ROW)) {
                        counterLines.set(i, counterLines.get(i) + 1);
                    }
                }
            }
        }

            /*
             * Now we know that our matrix has one line
             * But we cant delete the line if it is between two non-empty lines
             */
        if (counterLines.size() == 3 &&
                counterLines.get(1) == countLevels * width &&
                counterLines.get(0) < countLevels * width &&
                counterLines.get(2) < countLevels * width) {
            counterLines.set(1, 0);
        }

        //calculate count for empty columns
        for (int i = 0; i < width; i++) {
            if (counterColumns.size() < (i + 1)) {
                counterColumns.add(0);
            }
            for (int j = 0; j < height; j++) {
                for (List<String> currentLevel : recipe.values()) {
                    if (currentLevel.get(j * width + i).equals(EMPTY_ROW)) {
                        counterColumns.set(i, counterColumns.get(i) + 1);
                    }
                }
            }
        }


        if (counterColumns.size() == 3 &&
                counterColumns.get(1) == countLevels * height &&
                counterColumns.get(0) < countLevels * height &&
                counterColumns.get(2) < countLevels * height) {
            counterColumns.set(1, 0);
        }


        int countLines = 0;

        if (counterLines.isEmpty() && counterColumns.isEmpty()) {
            return this;
        }

        //create new matrix without empty lines
        for (int i = 0; i < height; i++) {
            if (counterLines.get(i) < countLevels * width) {
                for (String key : recipe.keySet()) {
                    if (!matrix.containsKey(key)) {
                        matrix.put(key, new ArrayList<String>());
                    }

                    for (int j = 0; j < height; j++) {
                        matrix.get(key).add(recipe.get(key).get(i * width + j));
                    }
                }
                countLines++;
            }
        }

            /*
             * Now we have this result:
             *
             * 1 0 1
             * 1 0 1
             *
             */

        int countColumns = width;


        //delete from the new matrix empty columns
        for (int i = 0, tCounter = 0; i < countColumns; i++, tCounter++) {
            if (counterColumns.get(tCounter) == countLevels * height) {
                for (String key : recipe.keySet()) {
                    if (!matrix.containsKey(key)) {
                        matrix.put(key, new ArrayList<String>());
                    }

                    for (int j = 0; j < countLines; j++) {
                        int t = j * (countColumns - 1) + i;
                        matrix.get(key).remove(t);
                    }
                }
                countColumns--;
                //counterColumns.set( i, counterColumns.get(i) - 1 );
                i--;
            }
        }


        return new RecipeMatrix(matrix, countColumns, countLines);
    }

        /*
         * Rotate matrix
         *
         * input:     output:
         *
         * 1 0 2      1 3 1
         * 3 4 8      8 4 0
         * 1 8 7      7 8 2
         *
         */

    public RecipeMatrix rotate() {

        RecipeMatrix rotatedMatrix = new RecipeMatrix(new HashMap<String, List<String>>());

        for (String key : recipe.keySet()) {
            ArrayList<String> buff = new ArrayList<String>();

            rotatedMatrix.recipe.put(key, buff);

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int index = height * (width - j - 1) + i;
                    buff.add(recipe.get(key).get(index < 0 ? 0 : index));
                }
            }


        }

        rotatedMatrix.width = height;
        rotatedMatrix.height = width;

        return rotatedMatrix;
    }

    public boolean equals(RecipeMatrix matrix) {

        if (recipe.size() != matrix.recipe.size()) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            if (matrix.width != width && matrix.height != height) {
                matrix = matrix.rotate();
                continue;
            }

            boolean found = true;

            for (String key : recipe.keySet()) {
                int trace1 = getTrace(key);
                int trace2 = matrix.getTrace(key);

                if (getTrace(key) != matrix.getTrace(key) || !recipe.get(key).equals(matrix.recipe.get(key))) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return true;
            }

            matrix = matrix.rotate();
        }

        for (String key : recipe.keySet()) {
            if (!matrix.recipe.containsKey(key)) {
                return false;
            }

        }

        return false;
    }

    public int getTrace(String level) {
        String trace = "";

        int min = Math.min(width, height);

        for (int i = 0; i < min; i++) {
            trace += recipe.get(level).get(i * min + i);
        }

        return trace.hashCode();
    }

    public RecipeMatrix clone() {
        return new RecipeMatrix(this.recipe, this.width, this.height);
    }
}
