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
package com.nuodb.migrator.cli;

import com.nuodb.migrator.cli.parse.*;
import com.nuodb.migrator.cli.parse.option.ArgumentImpl;
import com.nuodb.migrator.cli.parse.option.TriggerImpl;
import com.nuodb.migrator.cli.run.CliRun;
import com.nuodb.migrator.cli.run.CliRunLookup;
import com.nuodb.migrator.utils.PriorityList;
import com.nuodb.migrator.utils.SimplePriorityList;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Sergey Bushik
 */
public class CliCommand extends ArgumentImpl {

    private CliRunLookup cliRunLookup;

    @Override
    public PriorityList<Trigger> getTriggers() {
        PriorityList<Trigger> triggers = new SimplePriorityList<Trigger>();
        for (String command : getCliRunLookup().getCommands()) {
            triggers.add(new TriggerImpl(command));
        }
        return triggers;
    }

    @Override
    public boolean canProcess(CommandLine commandLine, String argument) {
        return getCliRunLookup().lookup(argument) != null;
    }

    @Override
    public void process(CommandLine commandLine, ListIterator<String> arguments) {
        CliRun cliRun = getCliRunLookup().lookup(arguments.next());
        cliRun.process(commandLine, arguments);
        commandLine.addValue(this, cliRun);
    }

    @Override
    public void postProcess(CommandLine commandLine) {
        super.postProcess(commandLine);
        CliRun cliRun = (CliRun) commandLine.getValue(this);
        try {
            if (cliRun != null) {
                cliRun.postProcess(commandLine);
            }
        } catch (OptionException exception) {
            throw new OptionException(cliRun, exception.getMessage());
        }
    }

    @Override
    public void help(StringBuilder buffer, Collection<HelpHint> hints, Comparator<Option> comparator) {
        super.help(buffer, hints, comparator);
    }

    @Override
    public List<Help> help(int indent, Collection<HelpHint> hints, Comparator<Option> comparator) {
        return super.help(indent, hints, comparator);
    }

    public CliRunLookup getCliRunLookup() {
        return cliRunLookup;
    }

    public void setCliRunLookup(CliRunLookup cliRunLookup) {
        this.cliRunLookup = cliRunLookup;
    }
}
