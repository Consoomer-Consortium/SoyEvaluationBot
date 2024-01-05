package com.rpi.soybot.command.impl;

import com.rpi.soybot.command.Command;
import com.rpi.soybot.command.CommandListener;
import com.rpi.soybot.database.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.sql.SQLException;

public class CommandForceEvaluateUser extends Command {

    public CommandForceEvaluateUser(CommandListener commandListener) {
        super("force", "Force a re-evaluation for a user", commandListener);
        this.getOptions().add(new OptionData(OptionType.USER, "user", "User to re-evaluate"));
    }

    @Override
    public void onUse(JDA jda, SlashCommandInteractionEvent event) throws SQLException {
        //TODO DRY
        Database database = this.getDatabase();

        User user = event.getOptions().get(0).getAsUser();

        String userId = user.getId();

        database.registerUser(userId);

        database.calculateAndSetUserScore(userId);

        int userScore = database.getUserScore(userId);

        if(userScore == Database.SCORE_FOR_NOT_FINISHED) {
            event.reply("They must finish the quiz with the `/assess` command to get a score!").queue();
        } else {
            //https://api.jdf2.org/consoomer/getLineGraphImage?userId=4&nickname=Jared&profileImageUrl=https://cdn.discordapp.com/avatars/120193744707256320/45efe68201c847a23808ba7b1549d37d.png&points=25

            String apiImageUrl = ("https://api.jdf2.org/consoomer/getLineGraphImage");
            apiImageUrl += ("?userId=" + userId);
            apiImageUrl += ("&nickname=@" + user.getName());
            apiImageUrl += ("&profileImageUrl=" + user.getAvatarUrl());
            apiImageUrl += ("&points=" + userScore);

            EmbedBuilder embedBuilder = new EmbedBuilder();

            embedBuilder.setTitle("Re-Evaluated Soy Chart for " + user.getName());
            embedBuilder.setColor(Color.GREEN);
            embedBuilder.setImage(apiImageUrl);

            event.replyEmbeds(embedBuilder.build()).queue();
        }
    }

}
