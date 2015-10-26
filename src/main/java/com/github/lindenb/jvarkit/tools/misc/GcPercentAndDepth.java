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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import htsjdk.tribble.readers.LineIterator;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.SequenceUtil;

import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.util.command.Command;
import com.github.lindenb.jvarkit.util.picard.GenomicSequence;
import com.github.lindenb.jvarkit.util.picard.MergingSamRecordIterator;
import com.github.lindenb.jvarkit.util.picard.SAMSequenceDictionaryProgress;
import com.github.lindenb.jvarkit.util.picard.SamFileReaderFactory;


/**
 * 
 * GcPercentAndDepth
 *
 */
public class GcPercentAndDepth extends AbstractGcPercentAndDepth
	{
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(GcPercentAndDepth.class);

	
	
	@Override
	public  Command createCommand() {
			return new MyCommand();
		}
		 
	public  static class MyCommand extends AbstractGcPercentAndDepth.AbstractGcPercentAndDepthCommand
	 	{		
	private SAMSequenceDictionary firstSamDict=null;

	
	/** A bed segment from the catpure */
	private class RegionCaptured
		implements Comparable<RegionCaptured>,Iterable<RegionCaptured.SlidingWindow>
		{
		int tid;
		int start0;
		int end0;
		
		public SAMSequenceRecord getSAMSequenceRecord()
			{
			return firstSamDict.getSequence(this.tid);
			}
		
		public String getChromosome()
			{
			return getSAMSequenceRecord().getSequenceName();
			}
		
		public int getStart()
			{
			return this.start0;
			}
		public int getEnd()
			{
			return this.end0;
			}

		public int length()
			{
			return this.getEnd()-this.getStart();
			}
		
		@Override
		public int compareTo(RegionCaptured o)
			{
			int i=tid-o.tid;
			if(i!=0) return i;
			i=start0-o.start0;
			if(i!=0) return i;
			i=end0-o.end0;
			if(i!=0) return i;
			return 0;
			}
		
		@Override
		public String toString() {
			return "roi:"+getChromosome()+":"+getStart()+"-"+getEnd();
			}
		
		Iterator<SlidingWindow> windows()
			{
			return new MyIter();
			}
		
		@Override
		public Iterator<SlidingWindow> iterator()
			{
			return windows();
			}
		
		private class MyIter
			implements Iterator<SlidingWindow>
			{
			int index_in_roi=0;
			private SlidingWindow make()
				{
				return new SlidingWindow(index_in_roi);
				}
			@Override
			public boolean hasNext()
				{
				return make().isValid();
				}
			@Override
			public SlidingWindow next()
				{
				SlidingWindow w=make();
				if(!w.isValid()) throw new IllegalStateException();
				this.index_in_roi++;
				return w;
				}
			
			@Override
			public void remove()
				{
				throw new UnsupportedOperationException();
				}
			}
		
		/** a Sliding window from the RegionCaptured */
		public class SlidingWindow
			{
			int index_in_roi;
			private SlidingWindow(int index_in_roi)
				{
				this.index_in_roi=index_in_roi;
				}
			
			public long getGenomicIndex()
				{
				long n=0;
				for(int t=0;t< RegionCaptured.this.tid;++t)
					{
					n+= firstSamDict.getSequence(t).getSequenceLength();
					}
				n+= this.getStart();
				return n;
				}
			
			public String getChromosome()
				{
				return RegionCaptured.this.getChromosome();
				}
			public int getStart()
				{
				return index_in_roi*windowStep + RegionCaptured.this.start0;
				}
			public int getEnd()
				{
				return Math.min(
					getStart()+windowSize ,
					RegionCaptured.this.getEnd()
					);
				}
			public int length()
				{
				return getEnd()-getStart();
				}
			boolean isValid()
				{
				if(getStart() >= RegionCaptured.this.getEnd())
					{
					return false;
					}
				if(getStart()+windowSize<   RegionCaptured.this.getEnd())
					{
					return true;
					}
				/** avoid side effect */
				if(getStart()+windowSize >= RegionCaptured.this.getSAMSequenceRecord().getSequenceLength())
					{
					return false;
					}
				int overhang=getStart()+windowSize-RegionCaptured.this.getEnd();
				return overhang>windowSize/2;
				}
			@Override
			public String toString() {
				return "win:"+getChromosome()+":"+getStart()+"-"+getEnd()+" "+this.index_in_roi;
				}
			}
		}
	
	
		@SuppressWarnings("resource")
		@Override
			public Collection<Throwable> call() throws Exception {
				
			
			if(this.windowSize<=0)
				{
				return wrapException("Bad window size.");
				}
			if(this.windowStep<=0)
				{
				return wrapException("Bad window step.");
				}
			
			if(refFile==null)
				{
				return wrapException("Undefined REF file");
				}
			
			final List<String> args = super.getInputFiles();
			
			if(args.isEmpty())
				{
				return wrapException("Illegal Number of arguments.");
				}
			IndexedFastaSequenceFile indexedFastaSequenceFile=null;
			List<SamReader> readers=new ArrayList<SamReader>();
	
			PrintWriter out=null;
			try
				{
				Map<String,String> resolveChromName=new HashMap<String, String>();
	
				LOG.info("Loading "+refFile);
				indexedFastaSequenceFile=new IndexedFastaSequenceFile(refFile);
				SAMSequenceDictionary dict= indexedFastaSequenceFile.getSequenceDictionary();
				if(dict==null)
					{
					return wrapException("Cannot get sequence dictionary for "+refFile+". "+
							this.getMessageBundle("picard.dictionary.needed"));
					}
				/* load chrom aliases */
				if(chromNameFile!=null)
					{
					LOG.info("Reading "+chromNameFile);
					LineIterator r=IOUtils.openFileForLineIterator(chromNameFile);
					while(r.hasNext())
						{
						String line=r.next();
						if(line.isEmpty()) continue;
						String tokens[]=line.split("[\t]");
						if(tokens.length<2)
							{
							LOG.warn("Bad conversion line (column count<2) in "+line);
							continue;
							}
						resolveChromName.put(tokens[0], tokens[1]);
						resolveChromName.put(tokens[1], tokens[0]);
						}
					CloserUtil.close(r);
					}
				
				out =  openFileOrStdoutAsPrintWriter();
				
				Set<String> all_samples=new TreeSet<String>();
				/* create input, collect sample names */
				for(int optind=0;
						optind< args.size();
						++optind)
					{
					File bamFile=new File(args.get(optind));
					LOG.info("Opening "+bamFile);
					SamReader samFileReaderScan=SamFileReaderFactory.mewInstance().open(bamFile);
					readers.add(samFileReaderScan);
					
					SAMFileHeader header= samFileReaderScan.getFileHeader();
					if(firstSamDict==null)
						{
						firstSamDict=header.getSequenceDictionary();
						}
					else if(!SequenceUtil.areSequenceDictionariesEqual(firstSamDict, header.getSequenceDictionary()))
						{
						return wrapException(getMessageBundle("not.the.same.sequence.dictionaries"));
						}
					
					for(SAMReadGroupRecord g: header.getReadGroups())
						{
						if(g.getSample()==null || g.getSample().isEmpty())
							{
							LOG.warn("Read group "+g.getId()+" has no sample in merged dictionary");
							continue;
							}
						all_samples.add(g.getSample());
						}
					}
				
				LOG.info("NSample:"+all_samples.size());
				
				/* print header */
				out.print("#");
				if(!super.disable_genomic_index)
					{
					out.print("id");
					out.print("\t");
					}
				out.print("chrom");
				out.print("\t");
				out.print("start");
				out.print("\t");
				out.print("end");
				out.print("\t");
				out.print("GCPercent");
				for(String sample:all_samples)
					{
					out.print("\t");
					out.print(sample);
					}
				out.println();
				
				
				List<RegionCaptured> regionsCaptured=new ArrayList<RegionCaptured>();
				if(bedFile!=null)
					{
					int N=0;
					LOG.warn("Reading BED:" +bedFile);
					Pattern tab=Pattern.compile("[\t]");
					LineIterator r=IOUtils.openFileForLineIterator(bedFile);
					while(r.hasNext())
						{
						String line=r.next();
						if(line.startsWith("#") || line.startsWith("browser") || line.startsWith("track") || line.isEmpty()) continue;
						String tokens[]=tab.split(line,4);
						if(tokens.length<3)
							{
							LOG.warn("No enough column in "+line+" "+bedFile);
							continue;
							}
						RegionCaptured roi=new RegionCaptured();
						roi.tid=dict.getSequenceIndex(tokens[0]);
						if(roi.tid==-1)
							{
							String altName= resolveChromName.get(tokens[0]);
							if(altName!=null)
									{
									LOG.info(tokens[0]+" was resolved to "+altName);
									roi.tid=firstSamDict.getSequenceIndex(altName);
									}
							else
									{
									LOG.warn("Cannot resolve "+tokens[0]+ " from "+new ArrayList<String>(resolveChromName.keySet()));
									}
							}
						
						
						if(roi.tid==-1)
							{
							LOG.warn("not in reference: chromosome "+tokens[0]+" in \""+line+"\" "+bedFile);
							continue;
							}
						roi.start0=Integer.parseInt(tokens[1]);
						roi.end0=Integer.parseInt(tokens[2]);
						regionsCaptured.add(roi);
						++N;
						}
					CloserUtil.close(r);
					LOG.info("end Reading BED:" +bedFile+"  N="+N);
					Collections.sort(regionsCaptured);
					}
				else
					{
					LOG.info("No capture, peeking everything");
					for(SAMSequenceRecord ssr:this.firstSamDict.getSequences())
						{
						RegionCaptured roi=new RegionCaptured();
						roi.tid=ssr.getSequenceIndex();
						roi.start0=0;
						roi.end0=ssr.getSequenceLength();
						regionsCaptured.add(roi);
						}
					}
				
				
				SAMSequenceDictionaryProgress progress=new SAMSequenceDictionaryProgress(firstSamDict);
				GenomicSequence genomicSequence=null;
				for(RegionCaptured roi:regionsCaptured)
					{
					if(out.checkError()) break;
					String chromName=roi.getChromosome();
					
					if(genomicSequence==null || !genomicSequence.getChrom().equals(chromName))
						{
						String altChromName=resolveChromName.get(chromName);
						if(altChromName!=null && genomicSequence!=null && genomicSequence.getChrom().equals(altChromName))
							{
							//ok, good chrom
							}
						else if(indexedFastaSequenceFile.getSequenceDictionary().getSequence(chromName)!=null)
							{
							genomicSequence=new GenomicSequence(indexedFastaSequenceFile,chromName);
							}
						else if(altChromName!=null  && indexedFastaSequenceFile.getSequenceDictionary().getSequence(altChromName)!=null)
							{
							genomicSequence=new GenomicSequence(indexedFastaSequenceFile,altChromName);
							}
						else
							{
							return wrapException(
									"when looking for genomic sequence "+getMessageBundle("chrom.missing.in.sequence.dictionary")+" "+
											chromName+
									" conversions available:"+resolveChromName);
							}
						}
					Map<String,int[]> sample2depth=new HashMap<String,int[]>();
					Map<String,Double> sample2meanDepth=new HashMap<String,Double>();
					for(String sample:all_samples)
						{
						int depth[]=new int[roi.length()];
						Arrays.fill(depth, 0);
						sample2depth.put(sample, depth);
						}
					List<CloseableIterator<SAMRecord>> iterators = new ArrayList<CloseableIterator<SAMRecord>>();
					for(SamReader r:readers)
						{
						iterators.add(r.query(roi.getChromosome(), roi.getStart()+1, roi.getEnd(), false));
						}
					
					Iterator<SAMRecord> merginIter=null;
					
					merginIter=null;
					if(iterators.isEmpty())
						{
						merginIter=Collections.emptyIterator();
						}
					else if(iterators.size()==1)
						{
						merginIter=iterators.get(0);
						}
					else
						{
						merginIter=new MergingSamRecordIterator(iterators);
						}
					while(merginIter.hasNext())
						{
						SAMRecord rec=merginIter.next();
						if(rec.getReadUnmappedFlag()) continue;
						if(rec.isSecondaryOrSupplementary()) continue;
						if(rec.getDuplicateReadFlag()) continue;
						if(rec.getReadFailsVendorQualityCheckFlag())  continue;
						if(rec.getMappingQuality()==0 || rec.getMappingQuality()==255) continue;
						
						SAMReadGroupRecord g=rec.getReadGroup();
						if(g==null) continue;
						progress.watch(rec);
						String sample=g.getSample();
						if(sample==null ) continue;
						int depth[]=sample2depth.get(sample);
						if(depth==null) continue;
						Cigar cigar=rec.getCigar();
						if(cigar==null) continue;
						
						int refpos1=rec.getAlignmentStart();
						
						for(CigarElement ce: cigar.getCigarElements())
							{
							CigarOperator op = ce.getOperator();
							if(!op.consumesReferenceBases() ) continue;
							if(op.consumesReadBases())
								{
								for(int i=0;i< ce.getLength();++i)
									{
									int pos0 = (refpos1+i-1);
									if(pos0< roi.getStart()) continue;
									if(pos0>= roi.getEnd()) continue;
									depth[pos0-roi.getStart()]++;
									}
								}
							refpos1 += ce.getLength();
							}
						}
					CloserUtil.close(merginIter);
					
					for(RegionCaptured.SlidingWindow win: roi)
						{
						double total=0f;
						int countN=0;
						for(int pos=win.getStart();pos<win.getEnd();++pos)
							{
							switch(genomicSequence.charAt(pos))
								{
								case 'c':case 'C':
								case 'g':case 'G':		
								case 's':case 'S':
									{
									total++;
									break;
									}
								case 'n':case 'N':countN++;break;
								default:break;
								}
							}
						if(skip_if_contains_N && countN>0) continue;
	 					double GCPercent=total/(double)win.length();
						
						int max_depth_for_win=0;
						sample2meanDepth.clear();
						for(String sample:all_samples)
							{
							int depth[]=sample2depth.get(sample);
							double sum=0;
							for(int pos=win.getStart();
									pos<win.getEnd() && (pos-roi.getStart())< depth.length;
									++pos)
								{
								sum+=depth[pos-roi.getStart()];
								}		
							double mean= (sum/(double)depth.length);
							max_depth_for_win=Math.max(max_depth_for_win, (int)mean);
							sample2meanDepth.put(sample,mean);
							}
						if(max_depth_for_win< this.min_depth) continue;
						if(!super.disable_genomic_index)
							{
							out.print(win.getGenomicIndex());
							out.print("\t");
							}
						out.print(win.getChromosome());
						out.print("\t");
						out.print(win.getStart());
						out.print("\t");
						out.print(win.getEnd());
						out.print("\t");
						out.printf("%.2f",GCPercent);
						
						for(String sample:all_samples)
							{
							out.print("\t");
							out.printf("%.2f",(double)sample2meanDepth.get(sample));
							}
						out.println();
						}
					}
					
				progress.finish();
				out.flush();
				return RETURN_OK;
				}
			catch(Exception err)
				{
				return wrapException(err);
				}
			finally
				{
				for(SamReader r:readers) CloserUtil.close(r);
				CloserUtil.close(indexedFastaSequenceFile);
				CloserUtil.close(out);
				}	
			}
	 	}
	
	public static void main(String[] args) {
		new GcPercentAndDepth().instanceMainWithExit(args);
		}
	}