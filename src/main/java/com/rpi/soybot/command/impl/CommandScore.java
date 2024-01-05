package com.rpi.soybot.command.impl;

import com.rpi.soybot.command.Command;
import com.rpi.soybot.command.CommandListener;
import com.rpi.soybot.database.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import javax.xml.crypto.Data;
import java.awt.*;
import java.sql.SQLException;

public class CommandScore extends Command {

    public CommandScore(CommandListener commandListener) {
        super("score", "Shows your score", commandListener);
    }

    @Override
    public void onUse(JDA jda, SlashCommandInteractionEvent event) throws SQLException {
        //TODO DRY
        Database database = this.getDatabase();

        String userId = event.getUser().getId();

        database.registerUser(userId);

        database.calculateAndSetUserScore(userId);

        int userScore = database.getUserScore(userId);

        if(userScore == Database.SCORE_FOR_NOT_FINISHED) {
            event.reply("You must finish the quiz with the `/assess` command to get a score!").queue();
        } else {
            //https://api.jdf2.org/consoomer/getLineGraphImage?userId=4&nickname=Jared&profileImageUrl=https://cdn.discordapp.com/avatars/120193744707256320/45efe68201c847a23808ba7b1549d37d.png&points=25

            String apiImageUrl = ("https://api.jdf2.org/consoomer/getLineGraphImage");
            apiImageUrl += ("?userId=" + userId);
            apiImageUrl += ("&nickname=@" + event.getUser().getName());
            apiImageUrl += ("&profileImageUrl=" + event.getUser().getAvatarUrl());
            apiImageUrl += ("&points=" + userScore);

            EmbedBuilder embedBuilder = new EmbedBuilder();

            embedBuilder.setTitle("Soy Chart for " + event.getUser().getName());
            embedBuilder.setColor(Color.GREEN);
            embedBuilder.setImage(apiImageUrl);

            event.replyEmbeds(embedBuilder.build()).queue();
        }
    }

}
