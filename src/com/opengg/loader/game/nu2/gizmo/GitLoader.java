package com.opengg.loader.game.nu2.gizmo;

import com.opengg.loader.game.nu2.gizmo.GitNode;
import com.opengg.loader.game.nu2.NU2MapData;

import java.nio.ByteBuffer;
import java.util.Scanner;

public class GitLoader {
    public static void loadGit(ByteBuffer fileBuffer, NU2MapData mapData) {
        Scanner scan = new Scanner(new String(fileBuffer.array()));
        int level = 0;
        GitNode node = new GitNode();
        String level1Tag = "";
        while (scan.hasNextLine()) {
            String token = scan.nextLine();
            String[] tokens = token.split(" ");
            if (token.contains("{")) {
                if (level == 0) {
                    node = new GitNode();
                    node.type = tokens[0];
                }
                if (level == 1) {
                    level1Tag = tokens[0];
                }
                level++;
                continue;
            } else if (token.contains("}")) {
                level--;
                if (level == 0) {
                    mapData.git().gitNodes().put(node.id, node);
                }
            }
            if (level == 1) {
                switch (tokens[0].trim()) {
                    case "BoxID" -> node.id = Integer.parseInt(tokens[1]);
                    case "Parent" -> {
                    }
                    case "Child" -> node.children.add(Integer.parseInt(tokens[1]));
                    case "x" -> node.x = Float.parseFloat(tokens[1]);
                    case "y" -> node.y = Float.parseFloat(tokens[1]);
                    case "Name" -> {
                        for (int i = 1; i < tokens.length; i++) {
                            node.name += " " + tokens[i];
                        }
                    }
                }
            } else {
                if (level == 2) {
                    switch (level1Tag.trim()) {
                        case "Condition" -> node.conditions.add(token);
                        case "Action" -> node.actions.add(token);
                        case "Gizmo" -> node.gizmos.put(token.trim().split(" ")[0], token.trim().split(" ").length == 1 ? "" : token.trim().split(" ")[1]);
                    }
                }
            }
        }
    }
}
