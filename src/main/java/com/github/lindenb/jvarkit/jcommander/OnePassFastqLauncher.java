/*
The MIT License (MIT)

Copyright (c) 2022 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/
package com.github.lindenb.jvarkit.jcommander;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Predicate;

import com.beust.jcommander.Parameter;
import com.github.lindenb.jvarkit.fastq.FastqPairedReaderFactory;
import com.github.lindenb.jvarkit.fastq.FastqPairedWriter;
import com.github.lindenb.jvarkit.fastq.FastqPairedWriterFactory;
import com.github.lindenb.jvarkit.fastq.FastqRecordPair;
import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.util.jcommander.Launcher;
import com.github.lindenb.jvarkit.util.log.Logger;

import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.fastq.FastqWriter;
import htsjdk.samtools.fastq.FastqWriterFactory;
import htsjdk.samtools.util.CloseableIterator;

public abstract class OnePassFastqLauncher extends Launcher {
private static final Logger LOG = Logger.build(OnePassFastqLauncher.class).make();
@Parameter(names={"-o","--out","-R1"},description="Output file for R1 fastq record or interleaved output."+OPT_OUPUT_FILE_OR_STDOUT)
private File outputFile1 = null;
@Parameter(names={"-R2"},description="Output file for R2 fastq record. If input is paired but R2 is omitted, output will be interleaved.")
private File outputFile2 = null;
@Parameter(names={"--paired"},description="assume input is paired end: we expect two fils, or the input is assumed interleaved fastq.")
boolean paired_end = false;
@Parameter(names={"-md5","--md5"},description="write md5 file")
private boolean write_md5=false;


protected Logger getLogger() {
	return LOG;
	}

protected int beforeFastq() {
	return 0;
	}
protected void afterFastq() {
	}

protected Predicate<FastqRecordPair> createPredicateForFastqRecordPair() {
	throw new UnsupportedOperationException();
	}
protected int runPairedEnd(CloseableIterator<FastqRecordPair> iter,FastqPairedWriter fws) throws IOException {
	final  Predicate<FastqRecordPair> predicate = this.createPredicateForFastqRecordPair();
	while(iter.hasNext()) {
		final FastqRecordPair pair= iter.next();
		if(predicate.test(pair)) {
			fws.write(pair);
			}
		}
	return 0;
	}

protected Predicate<FastqRecord> createPredicateForFastqRecord() {
	throw new UnsupportedOperationException();
	}
protected int runSingleEnd(FastqReader iter,FastqWriter fws) throws IOException {
	final  Predicate<FastqRecord> predicate = this.createPredicateForFastqRecord();
	while(iter.hasNext()) {
		final FastqRecord rec= iter.next();
		if(predicate.test(rec)) {
			fws.write(rec);
			}
		}
	return 0;
	}



@Override
public int doWork(final List<String> args)
	{
	try {
	if(beforeFastq()!=0) {
		getLogger().error("initialization failed.");
		return 1;
	}
	final int ret;
	if(paired_end || args.size()==2) {
		if(!paired_end && args.size()==2) {
			getLogger().warn("two files for input. Assuming paired-end input");
			}
	
		final FastqPairedReaderFactory fqpr = new FastqPairedReaderFactory();
		try(CloseableIterator<FastqRecordPair> iter=fqpr.open(args)) {
			FastqPairedWriterFactory fpwf = new FastqPairedWriterFactory();
			FastqPairedWriter fws = null;
			
			if(outputFile1!=null && outputFile2!=null) {
				fws = fpwf.open(outputFile1,outputFile2);
				}
			else if(outputFile1!=null && outputFile2==null) {
				fws = fpwf.open(outputFile1);
				}
			else if(outputFile1==null && outputFile2==null) 
				{
				fws = fpwf.open(new PrintStream(new BufferedOutputStream(stdout())));
				}
			else
				{
				LOG.error("bad output declaration.");
				return -1;
				}
			ret = runPairedEnd(iter, fws);
			fws.close();
			}
		}
	else
		{
		if(outputFile2!=null) {
			LOG.error("single end input but --R2 output file was specified.");
			return -1;
			}
		
		final String input = oneFileOrNull(null);
		try(FastqReader fqr= (input==null?
				new FastqReader(IOUtils.openStdinForBufferedReader()):
				new FastqReader(new File(input))
				)){
			try(FastqWriter fw = new FastqWriterFactory().newWriter(this.outputFile1)) {
				ret = runSingleEnd(fqr, fw);
				}
			}
			
		}
	return ret;
	} catch(final Throwable err) {
		
		try { afterFastq();} catch(final Throwable err2) {
			getLogger().warn(err2);
			}
		
		getLogger().error(err);
		return -1;
		}
	}

}
