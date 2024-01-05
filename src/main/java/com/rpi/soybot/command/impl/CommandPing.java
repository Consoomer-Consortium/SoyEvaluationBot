package com.rpi.soybot.command.impl;

import com.rpi.soybot.command.Command;
import com.rpi.soybot.command.CommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class CommandPing extends Command {

    public CommandPing(CommandListener commandListener) {
        super("ping", "Test command please ignore", commandListener);
    }

    @Override
    public void onUse(JDA jda, SlashCommandInteractionEvent event) {
        event.reply("jared #1 we stay winning").queue();
    }

}
