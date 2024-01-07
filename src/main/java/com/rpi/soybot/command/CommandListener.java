package com.rpi.soybot.command;

import com.rpi.soybot.command.impl.CommandAssess;
import com.rpi.soybot.command.impl.CommandForceEvaluateUser;
import com.rpi.soybot.command.impl.CommandPing;
import com.rpi.soybot.command.impl.CommandReload;
import com.rpi.soybot.command.impl.CommandScore;
import com.rpi.soybot.database.Database;
import com.rpi.soybot.database.Sqlite;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

public class CommandListener extends ListenerAdapter {

    private JDA jda;

    private HashMap<String, Command> commandRegistry = new HashMap<>();

    private static CommandListener INSTANCE = null;

    private Database database;

    public CommandListener(JDA jda, Database database) {
        this.jda = jda;
        this.database = database;

        this.registerCommands();
    }

    public void registerCommands() {
        //rpie guidId: 204621105720328193
        //jared guildId 744420915315605564
        String guildId = ("204621105720328193"); //TODO switch to global commands
        Guild guild = this.jda.getGuildById(guildId);

        this.registerCommand(new CommandPing(this), guild);
        this.registerCommand(new CommandAssess(this), guild);
        this.registerCommand(new CommandScore(this), guild);
        this.registerCommand(new CommandForceEvaluateUser(this), guild);
        this.registerCommand(new CommandReload(this), guild);

        guild.updateCommands().queue();
        //this.jda.updateCommands();
    }

    public void registerCommand(Command command, Guild guild) {
        this.commandRegistry.put(command.getName(), command);

        guild.upsertCommand(command.getName(), command.getDescription()).addOptions(command.getOptions()).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            String userId = event.getUser().getId();
            String username = event.getUser().getName();

            boolean clicked = true;

            if (event.getComponentId().equals(CommandAssess.BUTTON_ID_YES)) {
                this.database.setAnswerForUserCurrentQuestion(userId, username, true);
            }

            if (event.getComponentId().equals(CommandAssess.BUTTON_ID_NO)) {
                this.database.setAnswerForUserCurrentQuestion(userId, username, false);
            }

            if(clicked) {
                if(this.database.userHasAdditionalQuestionsAvailable(userId)) {
                    MessageEmbed embed = CommandAssess.buildEmbed("❓ Question", this.database.getNextQuestionForUser(userId).getQuestion(), "", this.database, userId);

                    event.deferEdit().queue();
                    event.getHook().editOriginalComponents().setActionRow(CommandAssess.getButtons()).setEmbeds(embed).queue();
                } else {
                    EmbedBuilder embedBuilder = new EmbedBuilder();

                    int score = this.database.calculateAndSetUserScore(userId);
                    embedBuilder.setTitle("Thanks for participating!");
                    embedBuilder.setFooter("Your score: " + score);
                    embedBuilder.setColor(Color.GREEN);

                    MessageEmbed embed = embedBuilder.build();

                    event.deferEdit().queue();
                    event.getHook().editOriginalComponents().setEmbeds(embed).queue();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            event.editMessage("There was an error!").queue();
        }
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            Command command = this.commandRegistry.get(event.getName());

            command.onUse(this.jda, event);
        } catch(Exception e) {
            e.printStackTrace();

            event.reply("There was an error!").queue();
        }
    }

    public static CommandListener getInstance(JDA jda) throws SQLException, IOException {
        if(INSTANCE == null) {
            INSTANCE = new CommandListener(jda, new Database(new Sqlite()));
            return INSTANCE;
        } else {
            return INSTANCE;
        }
    }

    public Database getDatabase() {
        return this.database;
    }

}
