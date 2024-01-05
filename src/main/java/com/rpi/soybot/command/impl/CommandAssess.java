package com.rpi.soybot.command.impl;

import com.rpi.soybot.command.Command;
import com.rpi.soybot.command.CommandListener;
import com.rpi.soybot.database.Database;
import com.rpi.soybot.database.Question;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import javax.xml.crypto.Data;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CommandAssess extends Command {

    public static final String BUTTON_ID_YES = ("yes");
    public static final String BUTTON_ID_NO = ("no");

    public static List<Button> buttons;

    public static TextInput textInput;
    public CommandAssess(CommandListener commandListener) {
        super("assess", "Assess your soyness", commandListener);
    }

    @Override
    public void onUse(JDA jda, SlashCommandInteractionEvent event) throws SQLException {
        Database database = this.getDatabase();
        String userId = event.getUser().getId();

        this.getDatabase().registerUser(userId);

        if(database.userHasAdditionalQuestionsAvailable(userId)) {
            Question nextQuestion = database.getNextQuestionForUser(userId);

            MessageEmbed embed = CommandAssess.buildEmbed("Question", nextQuestion.getQuestion(), nextQuestion.hasNote() ? nextQuestion.getQuestionNote() : "", this.getDatabase(), userId);

            event.replyEmbeds(embed).addActionRow(this.getButtons()).queue();
        } else {
            event.reply("You've completed all available questions! Check back later for more.").queue();
        }
    }

    public static MessageEmbed buildEmbed(String fieldTitle, String fieldContent, String note, Database database, String userId) throws SQLException {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        int remainingQuestions = database.getRemainingQuestionsForUser(userId);

        embedBuilder.setTitle("Soy Quiz - Remaining questions: " + remainingQuestions);

        embedBuilder.addField(fieldTitle, fieldContent, false);

        if(note.length() > 0) {
            embedBuilder.setFooter("\uD83D\uDDD2️ Note: " + note);
        }

        return embedBuilder.build();
    }

    public static List<Button> getButtons() {
        if(buttons == null) {
            buttons = new ArrayList<>();
            buttons.add(Button.primary(BUTTON_ID_YES, "Yes").withEmoji(Emoji.fromFormatted("<:babayep:991878077892264017>")));
            buttons.add(Button.primary(BUTTON_ID_NO, "No").withEmoji(Emoji.fromFormatted("<:nooooo:816535311147597844>")));
        }

        return buttons;
    }

}
