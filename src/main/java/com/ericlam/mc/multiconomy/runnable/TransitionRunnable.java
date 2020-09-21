package com.ericlam.mc.multiconomy.runnable;

import com.ericlam.mc.multiconomy.MultiConomy;
import com.ericlam.mc.multiconomy.UpdateResult;
import com.ericlam.mc.multiconomy.cache.CacheManager;
import com.ericlam.mc.multiconomy.cache.TransitionAction;
import com.ericlam.mc.multiconomy.cache.TransitionalType;
import com.ericlam.mc.multiconomy.sql.TableLockedException;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Queue;

/**
 * Created by caxerx on 2016/6/28.
 */
public class TransitionRunnable extends BukkitRunnable {

    private final Queue<TransitionAction> trans;
    private final CacheManager cacheManager;
    private int times;

    public TransitionRunnable(CacheManager cacheManager, Queue<TransitionAction> trans, int times) {
        this.trans = trans;
        this.times = times;
        this.cacheManager = cacheManager;
    }

    public TransitionRunnable(CacheManager cacheManager, Queue<TransitionAction> trans){
        this(cacheManager, trans, 5);
    }

    @Override
    public void run() {
        TransitionAction action = trans.poll();
        if (action != null) {
            double value = action.getValue();
            TransitionalType type = action.getType();
            OfflinePlayer player = action.getPlayer();
            if (type == TransitionalType.WITHDRAW) {
                value = -value;
            }
            try{
                var result = cacheManager.commitPlayerBalance(player, value, type == TransitionalType.SET, times <= 0);
                if (player.isOnline() && result == UpdateResult.SUCCESS) {
                    new CacheUpdateRunnable(cacheManager, player).runTaskAsynchronously(MultiConomy.getPlugin());
                }
            }catch (TableLockedException e){
                new TransitionRunnable(cacheManager, trans, --times).runTaskAsynchronously(MultiConomy.getPlugin());
            }
            //sqlController.logTransition(player.getUniqueId().toString(), operator, "WITHDRAW", value, System.currentTimeMillis(), connection, true);
        }
    }
}