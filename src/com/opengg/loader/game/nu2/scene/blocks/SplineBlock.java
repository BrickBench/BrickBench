package com.opengg.loader.game.nu2.scene.blocks;

import com.opengg.core.console.GGConsole;
import com.opengg.core.math.Vector3f;
import com.opengg.loader.game.nu2.NU2MapData;
import com.opengg.loader.game.nu2.scene.Spline;
import com.opengg.loader.loading.MapLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SplineBlock extends DefaultFileBlock {

    @Override
    public void readFromFile(ByteBuffer fileBuffer, long blockLength, int blockID, int blockOffset, NU2MapData mapData) throws IOException {
        super.readFromFile(fileBuffer, blockLength, blockID, blockOffset, mapData);

        int objCount = fileBuffer.getInt();
        fileBuffer.getInt();

        for (int i = 0; i < objCount; i++) {
            int addr = fileBuffer.position();

            short vectorCount = fileBuffer.getShort();
            String name;

            fileBuffer.getShort();
            int nameOffset = readPointer();

            name = switch (MapLoader.CURRENT_GAME_VERSION) {
                case LSW_TCS -> NameTableFileBlock.CURRENT.getByOffsetFromNameTable((nameOffset - NameTableFileBlock.CURRENT.blockOffset + 0x2c) & 0x0000FFFF);
                case LIJ1, LB1 -> NameTableFileBlock.CURRENT.getByOffsetFromStart((nameOffset + 0x2c) & 0x0000FFFF);
                default -> throw new IllegalStateException("Unexpected value: " + MapLoader.CURRENT_GAME_VERSION);
            };
            if (name == null) {
                name = Integer.toHexString(switch (MapLoader.CURRENT_GAME_VERSION) {
                    case LSW_TCS -> (nameOffset - NameTableFileBlock.CURRENT.blockOffset + 0x2c) & 0x0000FFFF;
                    case LIJ1, LB1 -> (nameOffset + 0x2c) & 0x0000FFFF;
                    default -> throw new IllegalStateException("Unexpected value: " + MapLoader.CURRENT_GAME_VERSION);
                });
                GGConsole.warn("Failed to find spline name");
            }


            name = name.toLowerCase();

            var vecs = new ArrayList<Vector3f>();
            for (int j = 0; j < vectorCount; j++) {
                var vec = new Vector3f(fileBuffer.getFloat(), fileBuffer.getFloat(), fileBuffer.getFloat());
                vecs.add(vec);
            }

            mapData.scene().splines().add(new Spline(name, vecs, addr));
        }
    }
}
