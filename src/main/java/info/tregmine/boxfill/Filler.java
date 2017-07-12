package info.tregmine.boxfill;

import info.tregmine.Tregmine;
import info.tregmine.api.GenericPlayer;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;

public class Filler extends AbstractFiller {
    private History history;
    private GenericPlayer player;
    private MaterialData item;
    private SavedBlocks currentJob;

    public Filler(Tregmine plugin, History history, GenericPlayer player, Block block1, Block block2,
                  MaterialData item, int workSize) {
        super(plugin, block1, block2, workSize);

        this.history = history;
        this.player = player;
        this.item = item;
        this.currentJob = new SavedBlocks();
    }

    @Override
    public void changeBlock(Block block) {
        currentJob.addBlock(block.getState());

        block.setType(item.getItemType());
        block.setData(item.getData());
    }

    @Override
    public void finished() {
        history.set(player, currentJob);
    }
}