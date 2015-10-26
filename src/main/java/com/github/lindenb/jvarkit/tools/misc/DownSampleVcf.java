/*
The MIT License (MIT)

Copyright (c) 2014 Pierre Lindenbaum

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


History:
* 2014 creation

*/
package com.github.lindenb.jvarkit.tools.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;


import htsjdk.samtools.util.CloserUtil;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.VCFHeader;

import com.github.lindenb.jvarkit.util.command.Command;
import com.github.lindenb.jvarkit.util.picard.SAMSequenceDictionaryProgress;
import com.github.lindenb.jvarkit.util.vcf.VcfIterator;


public class DownSampleVcf extends AbstractDownSampleVcf
	{
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(DownSampleVcf.class);

	@Override
	public Command createCommand() {
		return new MyCommand();
		}

	
	public  class MyCommand extends AbstractDownSampleVcf.AbstractDownSampleVcfCommand
	 	{		
		
		private  void doWork(VcfIterator in, VariantContextWriter out) throws IOException
			{
			final Random rand=new Random(this.seed);
			final List<VariantContext>  buffer=new ArrayList<VariantContext>(this.reservoir_size);
			final VCFHeader h2=new VCFHeader(in.getHeader());
			addMetaData(h2);
			final SAMSequenceDictionaryProgress progess=new SAMSequenceDictionaryProgress(in.getHeader());
			out.writeHeader(h2);
			if(this.reservoir_size!=0)
				{
				while(in.hasNext())
					{	
					if(buffer.size() < this.reservoir_size)
						{
						buffer.add(progess.watch(in.next()));
						}
					else
						{
						buffer.set(rand.nextInt(buffer.size()), progess.watch(in.next()));
						}
					}
				
				}
			for(VariantContext ctx:buffer)
				{
				out.add(ctx);
				}
			progess.finish();
			}
		
			@Override
			protected Collection<Throwable> call(String inputName) throws Exception {
				VcfIterator in = null;
				VariantContextWriter w= null;
				if(this.seed==-1L) seed = System.currentTimeMillis();
				try {
					in = openVcfIterator(inputName);
					w =  openVariantContextWriter();
					doWork(in, w);
					LOG.info("done");
					return RETURN_OK;
					}
				catch (Exception e)
					{
						return wrapException(e);
					}
				finally
					{
					CloserUtil.close(in);
					CloserUtil.close(w);
					}
		
				}
		
	 	}
	
	public static void main(String[] args)
		{
		new DownSampleVcf().instanceMainWithExit(args);
		}
	}