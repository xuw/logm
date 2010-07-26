package index;

import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;

import utils.LogFormatter;

import data.RegExprRecord;

public class EntryDocument {
	
	private static Logger LOG = LogFormatter.getLogger(EntryDocument.class);

	public static Document Document(RegExprRecord rec, int sequence) {

		// make a new, empty document
		Document doc = new Document();
		
		doc.add(new Field("regexpr", rec.RegExpr, Field.Store.YES, Field.Index.NO));
		String strindex = rec.RegExpr.replace("\\.\\*", "").replace("\\[0\\-9\\]\\+", "");
		doc.add(new Field("strindex", strindex, Field.Store.YES, Field.Index.TOKENIZED ) );
		doc.add(new Field("name", rec.nameMap, Field.Store.YES, Field.Index.NO));
		doc.add(new Field("field", rec.typeMap, Field.Store.YES, Field.Index.NO));
		String[] ids = rec.logid.split("-");
		doc.add(new Field("classname", ids[0], Field.Store.YES, Field.Index.TOKENIZED));
		doc.add(new Field("methodid", rec.caller_method, Field.Store.YES, Field.Index.UN_TOKENIZED));
		doc.add(new Field("line", ids[1], Field.Store.YES, Field.Index.TOKENIZED));
		doc.add(new Field("seq", Integer.toString(sequence),Field.Store.YES, Field.Index.UN_TOKENIZED)); 
		
		//LOG.fine(doc.toString());
		
		return doc;
	}

	private EntryDocument() {
	}

}
