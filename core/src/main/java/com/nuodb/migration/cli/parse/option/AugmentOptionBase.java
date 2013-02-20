/**
 * Copyright (c) 2012, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migration.cli.parse.option;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nuodb.migration.cli.parse.*;
import com.nuodb.migration.utils.PriorityList;
import com.nuodb.migration.utils.SimplePriorityList;

import java.util.*;

/**
 * Base subclass for the options augmented with an argument {@link Argument} and a child group {@link Group}.
 *
 * @author Sergey Bushik
 */
public abstract class AugmentOptionBase extends OptionBase implements AugmentOption {

    private Argument argument;
    private Group group;

    @Override
    public Argument getArgument() {
        return argument;
    }

    @Override
    public void setArgument(Argument argument) {
        this.argument = argument;
    }

    @Override
    public Group getGroup() {
        return group;
    }

    @Override
    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public Set<String> getPrefixes() {
        Set<String> prefixes = Sets.newHashSet();
        prefixes.addAll(getOptionPrefixes());
        if (getGroup() != null) {
            prefixes.addAll(getGroup().getPrefixes());
        }
        return prefixes;
    }

    @Override
    public PriorityList<Trigger> getTriggers() {
        PriorityList<Trigger> triggers = new SimplePriorityList<Trigger>(super.getTriggers());
        if (getGroup() != null) {
            triggers.addAll(getGroup().getTriggers());
        }
        return triggers;
    }

    @Override
    public boolean canProcess(CommandLine commandLine, String argument) {
        PriorityList<Trigger> triggers = getTriggers();
        if (getArgument() != null) {
            String argumentSeparator = getArgumentSeparator();
            if (argumentSeparator != null) {
                int index = argument.indexOf(argumentSeparator);
                if (index > 0) {
                    return findTrigger(triggers, argument.substring(0, index)) != null;
                }
            }
        }
        return findTrigger(triggers, argument) != null;
    }

    protected Trigger findTrigger(PriorityList<Trigger> triggers, String argument) {
        for (Trigger trigger : triggers) {
            if (trigger.fire(argument)) {
                return trigger;
            }
        }
        return null;
    }

    @Override
    public void defaults(CommandLine commandLine) {
        if (getArgument() != null) {
            getArgument().defaults(commandLine, this);
        }
        if (getGroup() != null) {
            getGroup().defaults(commandLine);
        }
    }

    @Override
    protected void preProcessOption(CommandLine commandLine, ListIterator<String> arguments) {
        String argumentSeparator = getArgumentSeparator();
        if (argumentSeparator != null) {
            String value = arguments.next();
            int index = value.indexOf(argumentSeparator);
            if (index > 0) {
                arguments.remove();
                arguments.add(value.substring(0, index));
                String quoted = quote(value.substring(index + 1));
                arguments.add(quoted);
                arguments.previous();
            }
            arguments.previous();
        }
    }

    public static String quote(String argument) {
        return "\"" + argument + "\"";
    }

    @Override
    public void process(CommandLine commandLine, ListIterator<String> arguments) {
        processArguments(commandLine, arguments);
        OptionProcessor optionProcessor = getOptionProcessor();
        if (optionProcessor != null) {
            optionProcessor.process(commandLine, this, arguments);
        }
        processAugment(commandLine, arguments);
        processGroup(commandLine, arguments);
    }

    protected void processAugment(CommandLine commandLine, ListIterator<String> arguments) {
        if (getArgument() != null && arguments.hasNext()) {
            String value = arguments.next();
            String unquoted = unquote(value);
            if (!value.equals(unquoted)) {
                arguments.remove();
                arguments.add(unquoted);
            }
            arguments.previous();
            getArgument().preProcess(commandLine, arguments);
            getArgument().process(commandLine, arguments, this);
        }
    }

    protected void processGroup(CommandLine commandLine, ListIterator<String> arguments) {
        if ((getGroup() != null) && getGroup().canProcess(commandLine, arguments)) {
            getGroup().process(commandLine, arguments);
        }
    }

    public static String unquote(String argument) {
        if (!argument.startsWith("\"") || !argument.endsWith("\"")) {
            return argument;
        }
        return argument.substring(1, argument.length() - 1);
    }

    @Override
    public void postProcess(CommandLine commandLine) throws OptionException {
        OptionProcessor optionProcessor = getOptionProcessor();
        if (optionProcessor != null) {
            optionProcessor.postProcess(commandLine, this);
        }
        postProcessOption(commandLine);
        if (commandLine.hasOption(this)) {
            postProcessArgument(commandLine);
            postProcessGroup(commandLine);
        }
    }

    protected void postProcessArgument(CommandLine commandLine) {
        if (getArgument() != null) {
            getArgument().postProcess(commandLine, this);
        }
    }

    protected void postProcessGroup(CommandLine commandLine) {
        if (getGroup() != null) {
            getGroup().postProcess(commandLine);
        }
    }

    @Override
    public void help(StringBuilder buffer, Collection<HelpHint> hints, Comparator<Option> comparator) {
        boolean displayArgument = (getArgument() != null) && hints.contains(HelpHint.AUGMENT_ARGUMENT);
        boolean displayGroup = (getGroup() != null) && hints.contains(HelpHint.AUGMENT_GROUP);
        if (displayArgument) {
            buffer.append(' ');
            getArgument().help(buffer, hints, comparator);
        }
        if (displayGroup) {
            buffer.append(' ');
            getGroup().help(buffer, hints, comparator);
        }
    }

    @Override
    public List<Help> help(int indent, Collection<HelpHint> hints, Comparator<Option> comparator) {
        List<Help> help = Lists.newArrayList();
        help.add(new HelpImpl(this, indent));
        Argument argument = getArgument();
        if (hints.contains(HelpHint.AUGMENT_ARGUMENT) && (argument != null)) {
            help.addAll(argument.help(indent + 1, hints, comparator));
        }
        Group children = getGroup();
        if (hints.contains(HelpHint.AUGMENT_GROUP) && (children != null)) {
            help.addAll(children.help(indent + 1, hints, comparator));
        }
        return help;
    }
}
