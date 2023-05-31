/*
 * Copyright 2017 Aurélien Gâteau <mail@agateau.com>
 *
 * This file is part of Pixel Wheels.
 *
 * Pixel Wheels is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.agateau.pixelwheels.gamesetup;

import com.agateau.pixelwheels.PwGame;
import com.agateau.pixelwheels.gameinput.EnoughGamepadsChecker;
import com.agateau.pixelwheels.rewards.Reward;
import com.agateau.pixelwheels.rewards.RewardManager;
import com.agateau.pixelwheels.screens.NavStageScreen;
import com.agateau.pixelwheels.screens.NotEnoughGamepadsScreen;
import com.agateau.pixelwheels.screens.UnlockedRewardScreen;
import com.agateau.utils.log.NLog;
import java.util.Set;

/** Orchestrate changes between screens for a game */
public abstract class Maestro implements EnoughGamepadsChecker.Listener {
    private final PwGame mGame;
    private final int mPlayerCount;
    private final EnoughGamepadsChecker mEnoughGamepadsChecker;

    private NotEnoughGamepadsScreen mNotEnoughGamepadsScreen;

    public Maestro(PwGame game, int playerCount) {
        mGame = game;
        mPlayerCount = playerCount;
        mEnoughGamepadsChecker = new EnoughGamepadsChecker(mGame.getConfig(), this);
        mEnoughGamepadsChecker.setInputCount(playerCount);
    }

    public abstract void start();

    public void stopEnoughGamepadChecker() {
        if (mNotEnoughGamepadsScreen != null) {
            hideNotEnoughGamepadsScreen();
        }
        mEnoughGamepadsChecker.setInputCount(0);
    }

    public int getPlayerCount() {
        return mPlayerCount;
    }

    protected PwGame getGame() {
        return mGame;
    }

    @Override
    public void onNotEnoughInputs() {
        NLog.e("There aren't enough connected gamepads");
        if (mNotEnoughGamepadsScreen == null) {
            mNotEnoughGamepadsScreen =
                    new NotEnoughGamepadsScreen(mGame, this, mEnoughGamepadsChecker);
            mGame.getScreenStack().showBlockingScreen(mNotEnoughGamepadsScreen);
        } else {
            mNotEnoughGamepadsScreen.updateMissingGamepads();
        }
    }

    @Override
    public void onEnoughInputs() {
        NLog.i("There are enough connected gamepads");
        hideNotEnoughGamepadsScreen();
    }

    private void hideNotEnoughGamepadsScreen() {
        mGame.getScreenStack().hideBlockingScreen();
        mNotEnoughGamepadsScreen = null;
    }

    void showUnlockedRewardScreen(final Runnable doAfterLastReward) {
        RewardManager manager = getGame().getRewardManager();
        final Set<Reward> rewards = manager.getUnseenUnlockedRewards();
        manager.markAllUnlockedRewardsSeen();
        showUnlockedRewardScreen(rewards, doAfterLastReward);
    }

    private void showUnlockedRewardScreen(
            final Set<Reward> rewards, final Runnable doAfterLastReward) {
        if (rewards.isEmpty()) {
            doAfterLastReward.run();
            return;
        }
        Reward reward = rewards.iterator().next();
        rewards.remove(reward);
        final NavStageScreen.NextListener navListener =
                new NavStageScreen.NextListener() {
                    @Override
                    public void onNextPressed() {
                        showUnlockedRewardScreen(rewards, doAfterLastReward);
                    }
                };
        getGame().replaceScreen(new UnlockedRewardScreen(getGame(), reward, navListener));
    }
}
