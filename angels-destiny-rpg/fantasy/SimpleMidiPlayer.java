package fantasy;
/*
 *	SimpleMidiPlayer.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
|<---            this code is formatted to fit into 80 columns             --->|
*/

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;




/**	<titleabbrev>SimpleMidiPlayer</titleabbrev>
	<title>Playing a MIDI file (easy)</title>

	<formalpara><title>Purpose</title>
	<para>Plays a single MIDI file.</para></formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis>
	<command>java SimpleMidiPlayer</command>
	<arg choice="plain"><replaceable>midifile</replaceable></arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><option><replaceable>midifile</replaceable></option></term>
	<listitem><para>the name of the MIDI file that should be
	played</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>

	<para>This program always uses the default Sequencer and the default
	Synthesizer to play on. For using non-default sequencers,
	synthesizers or to play on an external MIDI port, see
	<olink targetdoc="MidiPlayer"
	targetptr="MidiPlayer">MidiPlayer</olink>.</para>
	</formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="SimpleMidiPlayer.java.html">SimpleMidiPlayer.java</ulink>
	</para>
	</formalpara>

*/
public class SimpleMidiPlayer
{
	/*
	  These variables are not really intended to be static in a
	  meaning of (good) design. They are used by inner classes, so they
	  can't just be automatic variables. There were three possibilities:

	  a) make them instance variables and instantiate the object they
	  belong to. This is clean (and is how you should do it in a real
	  application), but would have made the example more complex.

	  b) make them automatic final variables inside main(). Design-wise,
	  this is better than static, but automatic final is something that
	  is still like some black magic to me.

	  c) make them static variables, as it is done here. This is quite bad
	  design, because if you have global variables, you can't easily do
	  the thing they are used for two times in concurrency without risking
	  indeterministic behaviour. However, it makes the example easy to
	  read.
	 */
	private static Sequencer	sm_sequencer = null;
	private static Synthesizer	sm_synthesizer = null;



	public static void playfile(String midifilename, int repeat)
	{
		/*
		 *	We check if there is no command-line argument at all
		 *	or the first one is '-h'.
		 *	If so, we display the usage message and
		 *	exit.
		 */

		String	strFilename = midifilename;
		File	midiFile = new File(strFilename);

		/*
		 *	We read in the MIDI file to a Sequence object.
		 *	This object is set at the Sequencer later.
		 */
		Sequence	sequence = null;
		try
		{
			sequence = MidiSystem.getSequence(midiFile);
		}
		catch (InvalidMidiDataException e)
		{
			/*
			 *	In case of an exception, we dump the exception
			 *	including the stack trace to the console.
			 *	Then, we exit the program.
			 */
			//e.printStackTrace();
			//System.exit(1);
			return;
		}
		catch (IOException e)
		{
			/*
			 *	In case of an exception, we dump the exception
			 *	including the stack trace to the console.
			 *	Then, we exit the program.
			 */
			//e.printStackTrace();
			//System.exit(1);
			return;
		}

		/*
		 *	Now, we need a Sequencer to play the sequence.
		 *	Here, we simply request the default sequencer.
		 */
		try
		{
			sm_sequencer = MidiSystem.getSequencer();
		}
		catch (MidiUnavailableException e)
		{
			//e.printStackTrace();
			//System.exit(1);
			return;
		}
		if (sm_sequencer == null)
		{
			//out("SimpleMidiPlayer.main(): can't get a Sequencer");
			//System.exit(1);
			return;
		}

		/*
		 *	There is a bug in the Sun jdk1.3/1.4.
		 *	It prevents correct termination of the VM.
		 *	So we have to exit ourselves.
		 *	To accomplish this, we register a Listener to the Sequencer.
		 *	It is called when there are "meta" events. Meta event
		 *	47 is end of track.
		 *
		 *	Thanks to Espen Riskedal for finding this trick.
		 */
		sm_sequencer.addMetaEventListener(new MetaEventListener()
			{
				public void meta(MetaMessage event)
				{
					if (event.getType() == 47)
					{
						sm_sequencer.close();
						if (sm_synthesizer != null)
						{
							sm_synthesizer.close();
						}
						//System.exit(0);
						return;
					}
				}
			});

		/*
		 *	The Sequencer is still a dead object.
		 *	We have to open() it to become live.
		 *	This is necessary to allocate some ressources in
		 *	the native part.
		 */
		try
		{
			sm_sequencer.open();
		}
		catch (MidiUnavailableException e)
		{
			//e.printStackTrace();
			//System.exit(1);
			return;
		}

		/*
		 *	Next step is to tell the Sequencer which
		 *	Sequence it has to play. In this case, we
		 *	set it as the Sequence object created above.
		 */
		try
		{
			sm_sequencer.setSequence(sequence);
		}
		catch (InvalidMidiDataException e)
		{
			//e.printStackTrace();
			//System.exit(1);
			return;
		}

		/*
		 *	Now, we set up the destinations the Sequence should be
		 *	played on. Here, we try to use the default
		 *	synthesizer. With some Java Sound implementations
		 *	(Sun jdk1.3/1.4 and others derived from this codebase),
		 *	the default sequencer and the default synthesizer
		 *	are combined in one object. We test for this
		 *	condition, and if it's true, nothing more has to
		 *	be done. With other implementations (namely Tritonus),
		 *	sequencers and synthesizers are always seperate
		 *	objects. In this case, we have to set up a link
		 *	between the two objects manually.
		 *
		 *	By the way, you should never rely on sequencers
		 *	being synthesizers, too; this is a highly non-
		 *	portable programming style. You should be able to
		 *	rely on the other case working. Alas, it is only
		 *	partly true for the Sun jdk1.3/1.4.
		 */
		if (! (sm_sequencer instanceof Synthesizer))
		{
			/*
			 *	We try to get the default synthesizer, open()
			 *	it and chain it to the sequencer with a
			 *	Transmitter-Receiver pair.
			 */
			try
			{
				sm_synthesizer = MidiSystem.getSynthesizer();
				sm_synthesizer.open();
				Receiver	synthReceiver = sm_synthesizer.getReceiver();
				Transmitter	seqTransmitter = sm_sequencer.getTransmitter();
				seqTransmitter.setReceiver(synthReceiver);
			}
			catch (MidiUnavailableException e)
			{
				e.printStackTrace();
			}
		}

		/*
		 *	Now, we can start over.
		 */
		sm_sequencer.start();

		if (repeat-- <= 0)
			return;
		playfile(midifilename, repeat);
	}



	private static void printUsageAndExit()
	{
		//out("SimpleMidiPlayer: usage:");
		//out("\tjava SimpleMidiPlayer <midifile>");
		//System.exit(1);
		return;
	}



	private static void out(String strMessage)
	{
		//System.out.println(strMessage);
	}
}



/*** SimpleMidiPlayer.java ***/
