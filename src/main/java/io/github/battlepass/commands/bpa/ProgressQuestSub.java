package io.github.battlepass.commands.bpa;

import io.github.battlepass.BattlePlugin;
import io.github.battlepass.cache.QuestCache;
import io.github.battlepass.commands.BpSubCommand;
import io.github.battlepass.controller.QuestController;
import io.github.battlepass.enums.Category;
import io.github.battlepass.objects.quests.Quest;
import io.github.battlepass.objects.user.User;
import io.github.battlepass.quests.workers.pipeline.steps.CompletionStep;
import io.github.battlepass.quests.workers.pipeline.steps.QuestValidationStep;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.stream.Collectors;

public class ProgressQuestSub extends BpSubCommand<CommandSender> {
    private final QuestCache questCache;
    private final QuestController controller;
    private final QuestValidationStep questValidationStep;
    private final CompletionStep completionStep;

    public ProgressQuestSub(BattlePlugin plugin) {
        super(plugin);
        this.questCache = plugin.getQuestCache();
        this.controller = plugin.getQuestController();
        this.questValidationStep = new QuestValidationStep(plugin);
        this.completionStep = new CompletionStep(plugin, this.questValidationStep);
        this.inheritPermission();
        this.addFlats("progress", "quest");
        this.addArgument(User.class, "player", sender -> Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .collect(Collectors.toList()));
        this.addArgument(Integer.class, "week");
        this.addArgument(String.class, "quest id");
        this.addArgument(Integer.class, "amount");
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        Optional<User> maybeUser = this.parseArgument(args, 2);
        Player player = maybeUser.map(value -> Bukkit.getPlayer(value.getUuid())).orElse(null);
        int week = this.parseArgument(args, 3);
        String id = this.parseArgument(args, 4);
        int amount = this.parseArgument(args, 5);

        if (!maybeUser.isPresent() || player == null) {
            this.lang.external("could-not-find-user", replacer -> replacer.set("player", args[2])).to(sender);
            return;
        }
        User user = maybeUser.get();
        Quest quest = this.questCache.getQuest(Category.WEEKLY.id(week), id);
        if (quest == null) {
            this.lang.local("invalid-quest-id", args[4], args[3].toLowerCase()).to(sender);
            return;
        }
        if (this.controller.isQuestDone(user, quest)) {
            this.lang.local("quest-already-done", args[2]);
            return;
        }
        this.questValidationStep.isQuestValid(player, user, quest, amount, false);
        this.completionStep.process(user, quest, this.controller.getQuestProgress(user, quest), amount, false);
        this.lang.local("successful-quest-progress", quest.getName()).to(sender);
    }
}