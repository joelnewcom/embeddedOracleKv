// Generated from /home/markos/KVS/gmf/kvstore/src/oracle/kv/impl/query/compiler/parser/KVQL.g4 by ANTLR 4.5.3
package oracle.kv.impl.query.compiler.parser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class KVQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, ACCOUNT=3, ADD=4, ADMIN=5, ALL=6, ALTER=7, AND=8, AS=9, 
		ASC=10, BY=11, CASE=12, CASCADE=13, CAST=14, COMMENT=15, CREATE=16, DECLARE=17, 
		DEFAULT=18, DESC=19, DESCRIBE=20, DROP=21, ELEMENTOF=22, ELSE=23, END=24, 
		ES_SHARDS=25, ES_REPLICAS=26, EXISTS=27, FIRST=28, FORCE_INDEX=29, FORCE_PRIMARY_INDEX=30, 
		FROM=31, FULLTEXT=32, GRANT=33, IDENTIFIED=34, IF=35, INDEX=36, INDEXES=37, 
		IS=38, JSON=39, KEY=40, KEYOF=41, KEYS=42, LAST=43, LIFETIME=44, LIMIT=45, 
		LOCK=46, MODIFY=47, NOT=48, NULLS=49, OFFSET=50, OF=51, ON=52, ONLY=53, 
		OR=54, ORDER=55, OVERRIDE=56, PASSWORD=57, PREFER_INDEXES=58, PREFER_PRIMARY_INDEX=59, 
		PRIMARY=60, REVOKE=61, ROLE=62, ROLES=63, SELECT=64, SHARD=65, SHOW=66, 
		TABLE=67, TABLES=68, THEN=69, TIME_UNIT=70, TO=71, TTL=72, TYPE=73, UNLOCK=74, 
		USER=75, USERS=76, USING=77, VALUES=78, WHEN=79, WHERE=80, ALL_PRIVILEGES=81, 
		IDENTIFIED_EXTERNALLY=82, PASSWORD_EXPIRE=83, RETAIN_CURRENT_PASSWORD=84, 
		CLEAR_RETAINED_PASSWORD=85, ARRAY_T=86, BINARY_T=87, BOOLEAN_T=88, DOUBLE_T=89, 
		ENUM_T=90, FLOAT_T=91, INTEGER_T=92, LONG_T=93, MAP_T=94, NUMBER_T=95, 
		RECORD_T=96, STRING_T=97, TIMESTAMP_T=98, ANY_T=99, ANYATOMIC_T=100, ANYJSONATOMIC_T=101, 
		ANYRECORD_T=102, SEMI=103, COMMA=104, COLON=105, LP=106, RP=107, LBRACK=108, 
		RBRACK=109, LBRACE=110, RBRACE=111, STAR=112, DOT=113, DOLLAR=114, QUESTION_MARK=115, 
		LT=116, LTE=117, GT=118, GTE=119, EQ=120, NEQ=121, LT_ANY=122, LTE_ANY=123, 
		GT_ANY=124, GTE_ANY=125, EQ_ANY=126, NEQ_ANY=127, PLUS=128, MINUS=129, 
		DIV=130, NULL=131, FALSE=132, TRUE=133, INT=134, FLOAT=135, DSTRING=136, 
		STRING=137, ID=138, BAD_ID=139, WS=140, C_COMMENT=141, LINE_COMMENT=142, 
		LINE_COMMENT1=143, UnrecognizedToken=144;
	public static final int
		RULE_parse = 0, RULE_statement = 1, RULE_query = 2, RULE_prolog = 3, RULE_var_decl = 4, 
		RULE_var_name = 5, RULE_expr = 6, RULE_sfw_expr = 7, RULE_from_clause = 8, 
		RULE_tab_alias = 9, RULE_where_clause = 10, RULE_select_clause = 11, RULE_hints = 12, 
		RULE_hint = 13, RULE_col_alias = 14, RULE_orderby_clause = 15, RULE_sort_spec = 16, 
		RULE_limit_clause = 17, RULE_offset_clause = 18, RULE_or_expr = 19, RULE_and_expr = 20, 
		RULE_not_expr = 21, RULE_cond_expr = 22, RULE_exists_expr = 23, RULE_is_of_type_expr = 24, 
		RULE_comp_expr = 25, RULE_comp_op = 26, RULE_any_op = 27, RULE_add_expr = 28, 
		RULE_multiply_expr = 29, RULE_unary_expr = 30, RULE_path_expr = 31, RULE_map_step = 32, 
		RULE_map_field_step = 33, RULE_map_filter_step = 34, RULE_array_step = 35, 
		RULE_array_slice_step = 36, RULE_array_filter_step = 37, RULE_primary_expr = 38, 
		RULE_column_ref = 39, RULE_const_expr = 40, RULE_var_ref = 41, RULE_array_constructor = 42, 
		RULE_map_constructor = 43, RULE_func_call = 44, RULE_case_expr = 45, RULE_cast_expr = 46, 
		RULE_parenthesized_expr = 47, RULE_quantified_type_def = 48, RULE_type_def = 49, 
		RULE_record_def = 50, RULE_field_def = 51, RULE_default_def = 52, RULE_default_value = 53, 
		RULE_not_null = 54, RULE_map_def = 55, RULE_array_def = 56, RULE_integer_def = 57, 
		RULE_json_def = 58, RULE_float_def = 59, RULE_string_def = 60, RULE_enum_def = 61, 
		RULE_boolean_def = 62, RULE_binary_def = 63, RULE_timestamp_def = 64, 
		RULE_any_def = 65, RULE_anyAtomic_def = 66, RULE_anyJsonAtomic_def = 67, 
		RULE_anyRecord_def = 68, RULE_name_path = 69, RULE_create_table_statement = 70, 
		RULE_table_name = 71, RULE_table_def = 72, RULE_key_def = 73, RULE_shard_key_def = 74, 
		RULE_id_list_with_size = 75, RULE_id_with_size = 76, RULE_storage_size = 77, 
		RULE_ttl_def = 78, RULE_alter_table_statement = 79, RULE_alter_def = 80, 
		RULE_alter_field_statements = 81, RULE_add_field_statement = 82, RULE_drop_field_statement = 83, 
		RULE_modify_field_statement = 84, RULE_schema_path = 85, RULE_init_schema_path_step = 86, 
		RULE_schema_path_step = 87, RULE_drop_table_statement = 88, RULE_create_index_statement = 89, 
		RULE_index_name = 90, RULE_index_path_list = 91, RULE_index_path = 92, 
		RULE_keys_expr = 93, RULE_values_expr = 94, RULE_brackets_expr = 95, RULE_create_text_index_statement = 96, 
		RULE_fts_field_list = 97, RULE_fts_path_list = 98, RULE_fts_path = 99, 
		RULE_es_properties = 100, RULE_es_property_assignment = 101, RULE_drop_index_statement = 102, 
		RULE_describe_statement = 103, RULE_schema_path_list = 104, RULE_show_statement = 105, 
		RULE_create_user_statement = 106, RULE_create_role_statement = 107, RULE_alter_user_statement = 108, 
		RULE_drop_user_statement = 109, RULE_drop_role_statement = 110, RULE_grant_statement = 111, 
		RULE_revoke_statement = 112, RULE_identifier_or_string = 113, RULE_identified_clause = 114, 
		RULE_create_user_identified_clause = 115, RULE_by_password = 116, RULE_password_lifetime = 117, 
		RULE_reset_password_clause = 118, RULE_account_lock = 119, RULE_grant_roles = 120, 
		RULE_grant_system_privileges = 121, RULE_grant_object_privileges = 122, 
		RULE_revoke_roles = 123, RULE_revoke_system_privileges = 124, RULE_revoke_object_privileges = 125, 
		RULE_principal = 126, RULE_sys_priv_list = 127, RULE_priv_item = 128, 
		RULE_obj_priv_list = 129, RULE_object = 130, RULE_json_text = 131, RULE_jsobject = 132, 
		RULE_jsarray = 133, RULE_jspair = 134, RULE_jsvalue = 135, RULE_comment = 136, 
		RULE_duration = 137, RULE_number = 138, RULE_string = 139, RULE_id_list = 140, 
		RULE_id = 141;
	public static final String[] ruleNames = {
		"parse", "statement", "query", "prolog", "var_decl", "var_name", "expr", 
		"sfw_expr", "from_clause", "tab_alias", "where_clause", "select_clause", 
		"hints", "hint", "col_alias", "orderby_clause", "sort_spec", "limit_clause", 
		"offset_clause", "or_expr", "and_expr", "not_expr", "cond_expr", "exists_expr", 
		"is_of_type_expr", "comp_expr", "comp_op", "any_op", "add_expr", "multiply_expr", 
		"unary_expr", "path_expr", "map_step", "map_field_step", "map_filter_step", 
		"array_step", "array_slice_step", "array_filter_step", "primary_expr", 
		"column_ref", "const_expr", "var_ref", "array_constructor", "map_constructor", 
		"func_call", "case_expr", "cast_expr", "parenthesized_expr", "quantified_type_def", 
		"type_def", "record_def", "field_def", "default_def", "default_value", 
		"not_null", "map_def", "array_def", "integer_def", "json_def", "float_def", 
		"string_def", "enum_def", "boolean_def", "binary_def", "timestamp_def", 
		"any_def", "anyAtomic_def", "anyJsonAtomic_def", "anyRecord_def", "name_path", 
		"create_table_statement", "table_name", "table_def", "key_def", "shard_key_def", 
		"id_list_with_size", "id_with_size", "storage_size", "ttl_def", "alter_table_statement", 
		"alter_def", "alter_field_statements", "add_field_statement", "drop_field_statement", 
		"modify_field_statement", "schema_path", "init_schema_path_step", "schema_path_step", 
		"drop_table_statement", "create_index_statement", "index_name", "index_path_list", 
		"index_path", "keys_expr", "values_expr", "brackets_expr", "create_text_index_statement", 
		"fts_field_list", "fts_path_list", "fts_path", "es_properties", "es_property_assignment", 
		"drop_index_statement", "describe_statement", "schema_path_list", "show_statement", 
		"create_user_statement", "create_role_statement", "alter_user_statement", 
		"drop_user_statement", "drop_role_statement", "grant_statement", "revoke_statement", 
		"identifier_or_string", "identified_clause", "create_user_identified_clause", 
		"by_password", "password_lifetime", "reset_password_clause", "account_lock", 
		"grant_roles", "grant_system_privileges", "grant_object_privileges", "revoke_roles", 
		"revoke_system_privileges", "revoke_object_privileges", "principal", "sys_priv_list", 
		"priv_item", "obj_priv_list", "object", "json_text", "jsobject", "jsarray", 
		"jspair", "jsvalue", "comment", "duration", "number", "string", "id_list", 
		"id"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'/*+'", "'*/'", null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, "';'", "','", "':'", "'('", 
		"')'", "'['", "']'", "'{'", "'}'", "'*'", "'.'", "'$'", "'?'", "'<'", 
		"'<='", "'>'", "'>='", "'='", "'!='", null, null, null, null, null, null, 
		"'+'", "'-'", "'/'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, "ACCOUNT", "ADD", "ADMIN", "ALL", "ALTER", "AND", "AS", 
		"ASC", "BY", "CASE", "CASCADE", "CAST", "COMMENT", "CREATE", "DECLARE", 
		"DEFAULT", "DESC", "DESCRIBE", "DROP", "ELEMENTOF", "ELSE", "END", "ES_SHARDS", 
		"ES_REPLICAS", "EXISTS", "FIRST", "FORCE_INDEX", "FORCE_PRIMARY_INDEX", 
		"FROM", "FULLTEXT", "GRANT", "IDENTIFIED", "IF", "INDEX", "INDEXES", "IS", 
		"JSON", "KEY", "KEYOF", "KEYS", "LAST", "LIFETIME", "LIMIT", "LOCK", "MODIFY", 
		"NOT", "NULLS", "OFFSET", "OF", "ON", "ONLY", "OR", "ORDER", "OVERRIDE", 
		"PASSWORD", "PREFER_INDEXES", "PREFER_PRIMARY_INDEX", "PRIMARY", "REVOKE", 
		"ROLE", "ROLES", "SELECT", "SHARD", "SHOW", "TABLE", "TABLES", "THEN", 
		"TIME_UNIT", "TO", "TTL", "TYPE", "UNLOCK", "USER", "USERS", "USING", 
		"VALUES", "WHEN", "WHERE", "ALL_PRIVILEGES", "IDENTIFIED_EXTERNALLY", 
		"PASSWORD_EXPIRE", "RETAIN_CURRENT_PASSWORD", "CLEAR_RETAINED_PASSWORD", 
		"ARRAY_T", "BINARY_T", "BOOLEAN_T", "DOUBLE_T", "ENUM_T", "FLOAT_T", "INTEGER_T", 
		"LONG_T", "MAP_T", "NUMBER_T", "RECORD_T", "STRING_T", "TIMESTAMP_T", 
		"ANY_T", "ANYATOMIC_T", "ANYJSONATOMIC_T", "ANYRECORD_T", "SEMI", "COMMA", 
		"COLON", "LP", "RP", "LBRACK", "RBRACK", "LBRACE", "RBRACE", "STAR", "DOT", 
		"DOLLAR", "QUESTION_MARK", "LT", "LTE", "GT", "GTE", "EQ", "NEQ", "LT_ANY", 
		"LTE_ANY", "GT_ANY", "GTE_ANY", "EQ_ANY", "NEQ_ANY", "PLUS", "MINUS", 
		"DIV", "NULL", "FALSE", "TRUE", "INT", "FLOAT", "DSTRING", "STRING", "ID", 
		"BAD_ID", "WS", "C_COMMENT", "LINE_COMMENT", "LINE_COMMENT1", "UnrecognizedToken"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "KVQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public KVQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ParseContext extends ParserRuleContext {
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode EOF() { return getToken(KVQLParser.EOF, 0); }
		public ParseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parse; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterParse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitParse(this);
		}
	}

	public final ParseContext parse() throws RecognitionException {
		ParseContext _localctx = new ParseContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_parse);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(284);
			statement();
			setState(285);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public Create_table_statementContext create_table_statement() {
			return getRuleContext(Create_table_statementContext.class,0);
		}
		public Create_index_statementContext create_index_statement() {
			return getRuleContext(Create_index_statementContext.class,0);
		}
		public Create_user_statementContext create_user_statement() {
			return getRuleContext(Create_user_statementContext.class,0);
		}
		public Create_role_statementContext create_role_statement() {
			return getRuleContext(Create_role_statementContext.class,0);
		}
		public Drop_index_statementContext drop_index_statement() {
			return getRuleContext(Drop_index_statementContext.class,0);
		}
		public Create_text_index_statementContext create_text_index_statement() {
			return getRuleContext(Create_text_index_statementContext.class,0);
		}
		public Drop_role_statementContext drop_role_statement() {
			return getRuleContext(Drop_role_statementContext.class,0);
		}
		public Drop_user_statementContext drop_user_statement() {
			return getRuleContext(Drop_user_statementContext.class,0);
		}
		public Alter_table_statementContext alter_table_statement() {
			return getRuleContext(Alter_table_statementContext.class,0);
		}
		public Alter_user_statementContext alter_user_statement() {
			return getRuleContext(Alter_user_statementContext.class,0);
		}
		public Drop_table_statementContext drop_table_statement() {
			return getRuleContext(Drop_table_statementContext.class,0);
		}
		public Grant_statementContext grant_statement() {
			return getRuleContext(Grant_statementContext.class,0);
		}
		public Revoke_statementContext revoke_statement() {
			return getRuleContext(Revoke_statementContext.class,0);
		}
		public Describe_statementContext describe_statement() {
			return getRuleContext(Describe_statementContext.class,0);
		}
		public Show_statementContext show_statement() {
			return getRuleContext(Show_statementContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitStatement(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(303);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				setState(287);
				query();
				}
				break;
			case 2:
				{
				setState(288);
				create_table_statement();
				}
				break;
			case 3:
				{
				setState(289);
				create_index_statement();
				}
				break;
			case 4:
				{
				setState(290);
				create_user_statement();
				}
				break;
			case 5:
				{
				setState(291);
				create_role_statement();
				}
				break;
			case 6:
				{
				setState(292);
				drop_index_statement();
				}
				break;
			case 7:
				{
				setState(293);
				create_text_index_statement();
				}
				break;
			case 8:
				{
				setState(294);
				drop_role_statement();
				}
				break;
			case 9:
				{
				setState(295);
				drop_user_statement();
				}
				break;
			case 10:
				{
				setState(296);
				alter_table_statement();
				}
				break;
			case 11:
				{
				setState(297);
				alter_user_statement();
				}
				break;
			case 12:
				{
				setState(298);
				drop_table_statement();
				}
				break;
			case 13:
				{
				setState(299);
				grant_statement();
				}
				break;
			case 14:
				{
				setState(300);
				revoke_statement();
				}
				break;
			case 15:
				{
				setState(301);
				describe_statement();
				}
				break;
			case 16:
				{
				setState(302);
				show_statement();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryContext extends ParserRuleContext {
		public Sfw_exprContext sfw_expr() {
			return getRuleContext(Sfw_exprContext.class,0);
		}
		public PrologContext prolog() {
			return getRuleContext(PrologContext.class,0);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitQuery(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_query);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(306);
			_la = _input.LA(1);
			if (_la==DECLARE) {
				{
				setState(305);
				prolog();
				}
			}

			setState(308);
			sfw_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrologContext extends ParserRuleContext {
		public TerminalNode DECLARE() { return getToken(KVQLParser.DECLARE, 0); }
		public List<Var_declContext> var_decl() {
			return getRuleContexts(Var_declContext.class);
		}
		public Var_declContext var_decl(int i) {
			return getRuleContext(Var_declContext.class,i);
		}
		public List<TerminalNode> SEMI() { return getTokens(KVQLParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(KVQLParser.SEMI, i);
		}
		public PrologContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prolog; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterProlog(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitProlog(this);
		}
	}

	public final PrologContext prolog() throws RecognitionException {
		PrologContext _localctx = new PrologContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_prolog);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			match(DECLARE);
			setState(311);
			var_decl();
			setState(312);
			match(SEMI);
			setState(318);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOLLAR) {
				{
				{
				setState(313);
				var_decl();
				setState(314);
				match(SEMI);
				}
				}
				setState(320);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Var_declContext extends ParserRuleContext {
		public Var_nameContext var_name() {
			return getRuleContext(Var_nameContext.class,0);
		}
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public Var_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var_decl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterVar_decl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitVar_decl(this);
		}
	}

	public final Var_declContext var_decl() throws RecognitionException {
		Var_declContext _localctx = new Var_declContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_var_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(321);
			var_name();
			setState(322);
			type_def();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Var_nameContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(KVQLParser.DOLLAR, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Var_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterVar_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitVar_name(this);
		}
	}

	public final Var_nameContext var_name() throws RecognitionException {
		Var_nameContext _localctx = new Var_nameContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_var_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(324);
			match(DOLLAR);
			setState(325);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public Or_exprContext or_expr() {
			return getRuleContext(Or_exprContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitExpr(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			or_expr(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Sfw_exprContext extends ParserRuleContext {
		public Select_clauseContext select_clause() {
			return getRuleContext(Select_clauseContext.class,0);
		}
		public From_clauseContext from_clause() {
			return getRuleContext(From_clauseContext.class,0);
		}
		public Where_clauseContext where_clause() {
			return getRuleContext(Where_clauseContext.class,0);
		}
		public Orderby_clauseContext orderby_clause() {
			return getRuleContext(Orderby_clauseContext.class,0);
		}
		public Limit_clauseContext limit_clause() {
			return getRuleContext(Limit_clauseContext.class,0);
		}
		public Offset_clauseContext offset_clause() {
			return getRuleContext(Offset_clauseContext.class,0);
		}
		public Sfw_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sfw_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSfw_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSfw_expr(this);
		}
	}

	public final Sfw_exprContext sfw_expr() throws RecognitionException {
		Sfw_exprContext _localctx = new Sfw_exprContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_sfw_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(329);
			select_clause();
			setState(330);
			from_clause();
			setState(332);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(331);
				where_clause();
				}
			}

			setState(335);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(334);
				orderby_clause();
				}
			}

			setState(338);
			_la = _input.LA(1);
			if (_la==LIMIT) {
				{
				setState(337);
				limit_clause();
				}
			}

			setState(341);
			_la = _input.LA(1);
			if (_la==OFFSET) {
				{
				setState(340);
				offset_clause();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class From_clauseContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(KVQLParser.FROM, 0); }
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public Tab_aliasContext tab_alias() {
			return getRuleContext(Tab_aliasContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<Var_nameContext> var_name() {
			return getRuleContexts(Var_nameContext.class);
		}
		public Var_nameContext var_name(int i) {
			return getRuleContext(Var_nameContext.class,i);
		}
		public List<TerminalNode> AS() { return getTokens(KVQLParser.AS); }
		public TerminalNode AS(int i) {
			return getToken(KVQLParser.AS, i);
		}
		public From_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_from_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFrom_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFrom_clause(this);
		}
	}

	public final From_clauseContext from_clause() throws RecognitionException {
		From_clauseContext _localctx = new From_clauseContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_from_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(343);
			match(FROM);
			setState(344);
			name_path();
			setState(349);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(346);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
				case 1:
					{
					setState(345);
					match(AS);
					}
					break;
				}
				setState(348);
				tab_alias();
				}
				break;
			}
			setState(360);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(351);
				match(COMMA);
				setState(352);
				expr();
				{
				setState(354);
				_la = _input.LA(1);
				if (_la==AS) {
					{
					setState(353);
					match(AS);
					}
				}

				setState(356);
				var_name();
				}
				}
				}
				setState(362);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tab_aliasContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode DOLLAR() { return getToken(KVQLParser.DOLLAR, 0); }
		public Tab_aliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tab_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterTab_alias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitTab_alias(this);
		}
	}

	public final Tab_aliasContext tab_alias() throws RecognitionException {
		Tab_aliasContext _localctx = new Tab_aliasContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_tab_alias);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(364);
			_la = _input.LA(1);
			if (_la==DOLLAR) {
				{
				setState(363);
				match(DOLLAR);
				}
			}

			setState(366);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Where_clauseContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(KVQLParser.WHERE, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Where_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_where_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterWhere_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitWhere_clause(this);
		}
	}

	public final Where_clauseContext where_clause() throws RecognitionException {
		Where_clauseContext _localctx = new Where_clauseContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_where_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(368);
			match(WHERE);
			setState(369);
			expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Select_clauseContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(KVQLParser.SELECT, 0); }
		public TerminalNode STAR() { return getToken(KVQLParser.STAR, 0); }
		public HintsContext hints() {
			return getRuleContext(HintsContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<Col_aliasContext> col_alias() {
			return getRuleContexts(Col_aliasContext.class);
		}
		public Col_aliasContext col_alias(int i) {
			return getRuleContext(Col_aliasContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Select_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_select_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSelect_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSelect_clause(this);
		}
	}

	public final Select_clauseContext select_clause() throws RecognitionException {
		Select_clauseContext _localctx = new Select_clauseContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_select_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(371);
			match(SELECT);
			setState(373);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(372);
				hints();
				}
			}

			setState(387);
			switch (_input.LA(1)) {
			case STAR:
				{
				setState(375);
				match(STAR);
				}
				break;
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case LP:
			case LBRACK:
			case LBRACE:
			case DOLLAR:
			case PLUS:
			case MINUS:
			case NULL:
			case FALSE:
			case TRUE:
			case INT:
			case FLOAT:
			case DSTRING:
			case STRING:
			case ID:
			case BAD_ID:
				{
				{
				setState(376);
				expr();
				setState(377);
				col_alias();
				setState(384);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(378);
					match(COMMA);
					setState(379);
					expr();
					setState(380);
					col_alias();
					}
					}
					setState(386);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HintsContext extends ParserRuleContext {
		public List<HintContext> hint() {
			return getRuleContexts(HintContext.class);
		}
		public HintContext hint(int i) {
			return getRuleContext(HintContext.class,i);
		}
		public HintsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hints; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterHints(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitHints(this);
		}
	}

	public final HintsContext hints() throws RecognitionException {
		HintsContext _localctx = new HintsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_hints);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(389);
			match(T__0);
			setState(393);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FORCE_INDEX) | (1L << FORCE_PRIMARY_INDEX) | (1L << PREFER_INDEXES) | (1L << PREFER_PRIMARY_INDEX))) != 0)) {
				{
				{
				setState(390);
				hint();
				}
				}
				setState(395);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(396);
			match(T__1);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HintContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(KVQLParser.STRING, 0); }
		public TerminalNode PREFER_INDEXES() { return getToken(KVQLParser.PREFER_INDEXES, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public TerminalNode FORCE_INDEX() { return getToken(KVQLParser.FORCE_INDEX, 0); }
		public List<Index_nameContext> index_name() {
			return getRuleContexts(Index_nameContext.class);
		}
		public Index_nameContext index_name(int i) {
			return getRuleContext(Index_nameContext.class,i);
		}
		public TerminalNode PREFER_PRIMARY_INDEX() { return getToken(KVQLParser.PREFER_PRIMARY_INDEX, 0); }
		public TerminalNode FORCE_PRIMARY_INDEX() { return getToken(KVQLParser.FORCE_PRIMARY_INDEX, 0); }
		public HintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterHint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitHint(this);
		}
	}

	public final HintContext hint() throws RecognitionException {
		HintContext _localctx = new HintContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_hint);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(425);
			switch (_input.LA(1)) {
			case PREFER_INDEXES:
				{
				{
				setState(398);
				match(PREFER_INDEXES);
				setState(399);
				match(LP);
				setState(400);
				name_path();
				setState(404);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)))) != 0) || _la==ID || _la==BAD_ID) {
					{
					{
					setState(401);
					index_name();
					}
					}
					setState(406);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(407);
				match(RP);
				}
				}
				break;
			case FORCE_INDEX:
				{
				{
				setState(409);
				match(FORCE_INDEX);
				setState(410);
				match(LP);
				setState(411);
				name_path();
				setState(412);
				index_name();
				setState(413);
				match(RP);
				}
				}
				break;
			case PREFER_PRIMARY_INDEX:
				{
				{
				setState(415);
				match(PREFER_PRIMARY_INDEX);
				setState(416);
				match(LP);
				setState(417);
				name_path();
				setState(418);
				match(RP);
				}
				}
				break;
			case FORCE_PRIMARY_INDEX:
				{
				{
				setState(420);
				match(FORCE_PRIMARY_INDEX);
				setState(421);
				match(LP);
				setState(422);
				name_path();
				setState(423);
				match(RP);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(428);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(427);
				match(STRING);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Col_aliasContext extends ParserRuleContext {
		public TerminalNode AS() { return getToken(KVQLParser.AS, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Col_aliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_col_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCol_alias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCol_alias(this);
		}
	}

	public final Col_aliasContext col_alias() throws RecognitionException {
		Col_aliasContext _localctx = new Col_aliasContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_col_alias);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(432);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(430);
				match(AS);
				setState(431);
				id();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Orderby_clauseContext extends ParserRuleContext {
		public TerminalNode ORDER() { return getToken(KVQLParser.ORDER, 0); }
		public TerminalNode BY() { return getToken(KVQLParser.BY, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<Sort_specContext> sort_spec() {
			return getRuleContexts(Sort_specContext.class);
		}
		public Sort_specContext sort_spec(int i) {
			return getRuleContext(Sort_specContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Orderby_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderby_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterOrderby_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitOrderby_clause(this);
		}
	}

	public final Orderby_clauseContext orderby_clause() throws RecognitionException {
		Orderby_clauseContext _localctx = new Orderby_clauseContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_orderby_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(434);
			match(ORDER);
			setState(435);
			match(BY);
			setState(436);
			expr();
			setState(437);
			sort_spec();
			setState(444);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(438);
				match(COMMA);
				setState(439);
				expr();
				setState(440);
				sort_spec();
				}
				}
				setState(446);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Sort_specContext extends ParserRuleContext {
		public TerminalNode NULLS() { return getToken(KVQLParser.NULLS, 0); }
		public TerminalNode ASC() { return getToken(KVQLParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(KVQLParser.DESC, 0); }
		public TerminalNode FIRST() { return getToken(KVQLParser.FIRST, 0); }
		public TerminalNode LAST() { return getToken(KVQLParser.LAST, 0); }
		public Sort_specContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sort_spec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSort_spec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSort_spec(this);
		}
	}

	public final Sort_specContext sort_spec() throws RecognitionException {
		Sort_specContext _localctx = new Sort_specContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_sort_spec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(448);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(447);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			setState(452);
			_la = _input.LA(1);
			if (_la==NULLS) {
				{
				setState(450);
				match(NULLS);
				setState(451);
				_la = _input.LA(1);
				if ( !(_la==FIRST || _la==LAST) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Limit_clauseContext extends ParserRuleContext {
		public TerminalNode LIMIT() { return getToken(KVQLParser.LIMIT, 0); }
		public Add_exprContext add_expr() {
			return getRuleContext(Add_exprContext.class,0);
		}
		public Limit_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limit_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterLimit_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitLimit_clause(this);
		}
	}

	public final Limit_clauseContext limit_clause() throws RecognitionException {
		Limit_clauseContext _localctx = new Limit_clauseContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_limit_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(454);
			match(LIMIT);
			setState(455);
			add_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Offset_clauseContext extends ParserRuleContext {
		public TerminalNode OFFSET() { return getToken(KVQLParser.OFFSET, 0); }
		public Add_exprContext add_expr() {
			return getRuleContext(Add_exprContext.class,0);
		}
		public Offset_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_offset_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterOffset_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitOffset_clause(this);
		}
	}

	public final Offset_clauseContext offset_clause() throws RecognitionException {
		Offset_clauseContext _localctx = new Offset_clauseContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_offset_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(457);
			match(OFFSET);
			setState(458);
			add_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Or_exprContext extends ParserRuleContext {
		public And_exprContext and_expr() {
			return getRuleContext(And_exprContext.class,0);
		}
		public Or_exprContext or_expr() {
			return getRuleContext(Or_exprContext.class,0);
		}
		public TerminalNode OR() { return getToken(KVQLParser.OR, 0); }
		public Or_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_or_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterOr_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitOr_expr(this);
		}
	}

	public final Or_exprContext or_expr() throws RecognitionException {
		return or_expr(0);
	}

	private Or_exprContext or_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Or_exprContext _localctx = new Or_exprContext(_ctx, _parentState);
		Or_exprContext _prevctx = _localctx;
		int _startState = 38;
		enterRecursionRule(_localctx, 38, RULE_or_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(461);
			and_expr(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(468);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,23,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Or_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_or_expr);
					setState(463);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(464);
					match(OR);
					setState(465);
					and_expr(0);
					}
					} 
				}
				setState(470);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,23,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class And_exprContext extends ParserRuleContext {
		public Not_exprContext not_expr() {
			return getRuleContext(Not_exprContext.class,0);
		}
		public And_exprContext and_expr() {
			return getRuleContext(And_exprContext.class,0);
		}
		public TerminalNode AND() { return getToken(KVQLParser.AND, 0); }
		public And_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_and_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnd_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnd_expr(this);
		}
	}

	public final And_exprContext and_expr() throws RecognitionException {
		return and_expr(0);
	}

	private And_exprContext and_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		And_exprContext _localctx = new And_exprContext(_ctx, _parentState);
		And_exprContext _prevctx = _localctx;
		int _startState = 40;
		enterRecursionRule(_localctx, 40, RULE_and_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(472);
			not_expr();
			}
			_ctx.stop = _input.LT(-1);
			setState(479);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new And_exprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_and_expr);
					setState(474);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(475);
					match(AND);
					setState(476);
					not_expr();
					}
					} 
				}
				setState(481);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Not_exprContext extends ParserRuleContext {
		public Cond_exprContext cond_expr() {
			return getRuleContext(Cond_exprContext.class,0);
		}
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public Not_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_not_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterNot_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitNot_expr(this);
		}
	}

	public final Not_exprContext not_expr() throws RecognitionException {
		Not_exprContext _localctx = new Not_exprContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_not_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(483);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(482);
				match(NOT);
				}
				break;
			}
			setState(485);
			cond_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Cond_exprContext extends ParserRuleContext {
		public Comp_exprContext comp_expr() {
			return getRuleContext(Comp_exprContext.class,0);
		}
		public Exists_exprContext exists_expr() {
			return getRuleContext(Exists_exprContext.class,0);
		}
		public Is_of_type_exprContext is_of_type_expr() {
			return getRuleContext(Is_of_type_exprContext.class,0);
		}
		public Cond_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cond_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCond_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCond_expr(this);
		}
	}

	public final Cond_exprContext cond_expr() throws RecognitionException {
		Cond_exprContext _localctx = new Cond_exprContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_cond_expr);
		try {
			setState(490);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(487);
				comp_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(488);
				exists_expr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(489);
				is_of_type_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Exists_exprContext extends ParserRuleContext {
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public Add_exprContext add_expr() {
			return getRuleContext(Add_exprContext.class,0);
		}
		public Exists_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exists_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterExists_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitExists_expr(this);
		}
	}

	public final Exists_exprContext exists_expr() throws RecognitionException {
		Exists_exprContext _localctx = new Exists_exprContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_exists_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(492);
			match(EXISTS);
			setState(493);
			add_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Is_of_type_exprContext extends ParserRuleContext {
		public Add_exprContext add_expr() {
			return getRuleContext(Add_exprContext.class,0);
		}
		public TerminalNode IS() { return getToken(KVQLParser.IS, 0); }
		public TerminalNode OF() { return getToken(KVQLParser.OF, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public List<Quantified_type_defContext> quantified_type_def() {
			return getRuleContexts(Quantified_type_defContext.class);
		}
		public Quantified_type_defContext quantified_type_def(int i) {
			return getRuleContext(Quantified_type_defContext.class,i);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public TerminalNode TYPE() { return getToken(KVQLParser.TYPE, 0); }
		public List<TerminalNode> ONLY() { return getTokens(KVQLParser.ONLY); }
		public TerminalNode ONLY(int i) {
			return getToken(KVQLParser.ONLY, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Is_of_type_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_is_of_type_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterIs_of_type_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitIs_of_type_expr(this);
		}
	}

	public final Is_of_type_exprContext is_of_type_expr() throws RecognitionException {
		Is_of_type_exprContext _localctx = new Is_of_type_exprContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_is_of_type_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(495);
			add_expr();
			setState(496);
			match(IS);
			setState(498);
			_la = _input.LA(1);
			if (_la==NOT) {
				{
				setState(497);
				match(NOT);
				}
			}

			setState(500);
			match(OF);
			setState(502);
			_la = _input.LA(1);
			if (_la==TYPE) {
				{
				setState(501);
				match(TYPE);
				}
			}

			setState(504);
			match(LP);
			setState(506);
			_la = _input.LA(1);
			if (_la==ONLY) {
				{
				setState(505);
				match(ONLY);
				}
			}

			setState(508);
			quantified_type_def();
			setState(516);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(509);
				match(COMMA);
				setState(511);
				_la = _input.LA(1);
				if (_la==ONLY) {
					{
					setState(510);
					match(ONLY);
					}
				}

				setState(513);
				quantified_type_def();
				}
				}
				setState(518);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(519);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Comp_exprContext extends ParserRuleContext {
		public List<Add_exprContext> add_expr() {
			return getRuleContexts(Add_exprContext.class);
		}
		public Add_exprContext add_expr(int i) {
			return getRuleContext(Add_exprContext.class,i);
		}
		public Comp_opContext comp_op() {
			return getRuleContext(Comp_opContext.class,0);
		}
		public Any_opContext any_op() {
			return getRuleContext(Any_opContext.class,0);
		}
		public Comp_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comp_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterComp_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitComp_expr(this);
		}
	}

	public final Comp_exprContext comp_expr() throws RecognitionException {
		Comp_exprContext _localctx = new Comp_exprContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_comp_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(521);
			add_expr();
			setState(528);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				{
				setState(524);
				switch (_input.LA(1)) {
				case LT:
				case LTE:
				case GT:
				case GTE:
				case EQ:
				case NEQ:
					{
					setState(522);
					comp_op();
					}
					break;
				case LT_ANY:
				case LTE_ANY:
				case GT_ANY:
				case GTE_ANY:
				case EQ_ANY:
				case NEQ_ANY:
					{
					setState(523);
					any_op();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(526);
				add_expr();
				}
				break;
			}
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Comp_opContext extends ParserRuleContext {
		public TerminalNode EQ() { return getToken(KVQLParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(KVQLParser.NEQ, 0); }
		public TerminalNode GT() { return getToken(KVQLParser.GT, 0); }
		public TerminalNode GTE() { return getToken(KVQLParser.GTE, 0); }
		public TerminalNode LT() { return getToken(KVQLParser.LT, 0); }
		public TerminalNode LTE() { return getToken(KVQLParser.LTE, 0); }
		public Comp_opContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comp_op; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterComp_op(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitComp_op(this);
		}
	}

	public final Comp_opContext comp_op() throws RecognitionException {
		Comp_opContext _localctx = new Comp_opContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_comp_op);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(530);
			_la = _input.LA(1);
			if ( !(((((_la - 116)) & ~0x3f) == 0 && ((1L << (_la - 116)) & ((1L << (LT - 116)) | (1L << (LTE - 116)) | (1L << (GT - 116)) | (1L << (GTE - 116)) | (1L << (EQ - 116)) | (1L << (NEQ - 116)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Any_opContext extends ParserRuleContext {
		public TerminalNode EQ_ANY() { return getToken(KVQLParser.EQ_ANY, 0); }
		public TerminalNode NEQ_ANY() { return getToken(KVQLParser.NEQ_ANY, 0); }
		public TerminalNode GT_ANY() { return getToken(KVQLParser.GT_ANY, 0); }
		public TerminalNode GTE_ANY() { return getToken(KVQLParser.GTE_ANY, 0); }
		public TerminalNode LT_ANY() { return getToken(KVQLParser.LT_ANY, 0); }
		public TerminalNode LTE_ANY() { return getToken(KVQLParser.LTE_ANY, 0); }
		public Any_opContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_any_op; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAny_op(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAny_op(this);
		}
	}

	public final Any_opContext any_op() throws RecognitionException {
		Any_opContext _localctx = new Any_opContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_any_op);
		try {
			setState(538);
			switch (_input.LA(1)) {
			case EQ_ANY:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(532);
				match(EQ_ANY);
				}
				}
				break;
			case NEQ_ANY:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(533);
				match(NEQ_ANY);
				}
				}
				break;
			case GT_ANY:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(534);
				match(GT_ANY);
				}
				}
				break;
			case GTE_ANY:
				enterOuterAlt(_localctx, 4);
				{
				{
				setState(535);
				match(GTE_ANY);
				}
				}
				break;
			case LT_ANY:
				enterOuterAlt(_localctx, 5);
				{
				{
				setState(536);
				match(LT_ANY);
				}
				}
				break;
			case LTE_ANY:
				enterOuterAlt(_localctx, 6);
				{
				{
				setState(537);
				match(LTE_ANY);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Add_exprContext extends ParserRuleContext {
		public List<Multiply_exprContext> multiply_expr() {
			return getRuleContexts(Multiply_exprContext.class);
		}
		public Multiply_exprContext multiply_expr(int i) {
			return getRuleContext(Multiply_exprContext.class,i);
		}
		public List<TerminalNode> PLUS() { return getTokens(KVQLParser.PLUS); }
		public TerminalNode PLUS(int i) {
			return getToken(KVQLParser.PLUS, i);
		}
		public List<TerminalNode> MINUS() { return getTokens(KVQLParser.MINUS); }
		public TerminalNode MINUS(int i) {
			return getToken(KVQLParser.MINUS, i);
		}
		public Add_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_add_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAdd_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAdd_expr(this);
		}
	}

	public final Add_exprContext add_expr() throws RecognitionException {
		Add_exprContext _localctx = new Add_exprContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_add_expr);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(540);
			multiply_expr();
			setState(545);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(541);
					_la = _input.LA(1);
					if ( !(_la==PLUS || _la==MINUS) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					setState(542);
					multiply_expr();
					}
					} 
				}
				setState(547);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Multiply_exprContext extends ParserRuleContext {
		public List<Unary_exprContext> unary_expr() {
			return getRuleContexts(Unary_exprContext.class);
		}
		public Unary_exprContext unary_expr(int i) {
			return getRuleContext(Unary_exprContext.class,i);
		}
		public List<TerminalNode> STAR() { return getTokens(KVQLParser.STAR); }
		public TerminalNode STAR(int i) {
			return getToken(KVQLParser.STAR, i);
		}
		public List<TerminalNode> DIV() { return getTokens(KVQLParser.DIV); }
		public TerminalNode DIV(int i) {
			return getToken(KVQLParser.DIV, i);
		}
		public Multiply_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiply_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMultiply_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMultiply_expr(this);
		}
	}

	public final Multiply_exprContext multiply_expr() throws RecognitionException {
		Multiply_exprContext _localctx = new Multiply_exprContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_multiply_expr);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(548);
			unary_expr();
			setState(553);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(549);
					_la = _input.LA(1);
					if ( !(_la==STAR || _la==DIV) ) {
					_errHandler.recoverInline(this);
					} else {
						consume();
					}
					setState(550);
					unary_expr();
					}
					} 
				}
				setState(555);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Unary_exprContext extends ParserRuleContext {
		public Path_exprContext path_expr() {
			return getRuleContext(Path_exprContext.class,0);
		}
		public Unary_exprContext unary_expr() {
			return getRuleContext(Unary_exprContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(KVQLParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(KVQLParser.MINUS, 0); }
		public Unary_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unary_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterUnary_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitUnary_expr(this);
		}
	}

	public final Unary_exprContext unary_expr() throws RecognitionException {
		Unary_exprContext _localctx = new Unary_exprContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_unary_expr);
		int _la;
		try {
			setState(559);
			switch (_input.LA(1)) {
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case LP:
			case LBRACK:
			case LBRACE:
			case DOLLAR:
			case NULL:
			case FALSE:
			case TRUE:
			case INT:
			case FLOAT:
			case DSTRING:
			case STRING:
			case ID:
			case BAD_ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(556);
				path_expr();
				}
				break;
			case PLUS:
			case MINUS:
				enterOuterAlt(_localctx, 2);
				{
				setState(557);
				_la = _input.LA(1);
				if ( !(_la==PLUS || _la==MINUS) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				setState(558);
				unary_expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Path_exprContext extends ParserRuleContext {
		public Primary_exprContext primary_expr() {
			return getRuleContext(Primary_exprContext.class,0);
		}
		public List<Map_stepContext> map_step() {
			return getRuleContexts(Map_stepContext.class);
		}
		public Map_stepContext map_step(int i) {
			return getRuleContext(Map_stepContext.class,i);
		}
		public List<Array_stepContext> array_step() {
			return getRuleContexts(Array_stepContext.class);
		}
		public Array_stepContext array_step(int i) {
			return getRuleContext(Array_stepContext.class,i);
		}
		public Path_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_path_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterPath_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitPath_expr(this);
		}
	}

	public final Path_exprContext path_expr() throws RecognitionException {
		Path_exprContext _localctx = new Path_exprContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_path_expr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(561);
			primary_expr();
			setState(566);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(564);
					switch (_input.LA(1)) {
					case DOT:
						{
						setState(562);
						map_step();
						}
						break;
					case LBRACK:
						{
						setState(563);
						array_step();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(568);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Map_stepContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(KVQLParser.DOT, 0); }
		public Map_filter_stepContext map_filter_step() {
			return getRuleContext(Map_filter_stepContext.class,0);
		}
		public Map_field_stepContext map_field_step() {
			return getRuleContext(Map_field_stepContext.class,0);
		}
		public Map_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMap_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMap_step(this);
		}
	}

	public final Map_stepContext map_step() throws RecognitionException {
		Map_stepContext _localctx = new Map_stepContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_map_step);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(569);
			match(DOT);
			setState(572);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				{
				setState(570);
				map_filter_step();
				}
				break;
			case 2:
				{
				setState(571);
				map_field_step();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Map_field_stepContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public Var_refContext var_ref() {
			return getRuleContext(Var_refContext.class,0);
		}
		public Parenthesized_exprContext parenthesized_expr() {
			return getRuleContext(Parenthesized_exprContext.class,0);
		}
		public Func_callContext func_call() {
			return getRuleContext(Func_callContext.class,0);
		}
		public Map_field_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_field_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMap_field_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMap_field_step(this);
		}
	}

	public final Map_field_stepContext map_field_step() throws RecognitionException {
		Map_field_stepContext _localctx = new Map_field_stepContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_map_field_step);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(579);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				{
				setState(574);
				id();
				}
				break;
			case 2:
				{
				setState(575);
				string();
				}
				break;
			case 3:
				{
				setState(576);
				var_ref();
				}
				break;
			case 4:
				{
				setState(577);
				parenthesized_expr();
				}
				break;
			case 5:
				{
				setState(578);
				func_call();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Map_filter_stepContext extends ParserRuleContext {
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public TerminalNode KEYS() { return getToken(KVQLParser.KEYS, 0); }
		public TerminalNode VALUES() { return getToken(KVQLParser.VALUES, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Map_filter_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_filter_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMap_filter_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMap_filter_step(this);
		}
	}

	public final Map_filter_stepContext map_filter_step() throws RecognitionException {
		Map_filter_stepContext _localctx = new Map_filter_stepContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_map_filter_step);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(581);
			_la = _input.LA(1);
			if ( !(_la==KEYS || _la==VALUES) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(582);
			match(LP);
			setState(584);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)) | (1L << (LP - 64)) | (1L << (LBRACK - 64)) | (1L << (LBRACE - 64)) | (1L << (DOLLAR - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (PLUS - 128)) | (1L << (MINUS - 128)) | (1L << (NULL - 128)) | (1L << (FALSE - 128)) | (1L << (TRUE - 128)) | (1L << (INT - 128)) | (1L << (FLOAT - 128)) | (1L << (DSTRING - 128)) | (1L << (STRING - 128)) | (1L << (ID - 128)) | (1L << (BAD_ID - 128)))) != 0)) {
				{
				setState(583);
				expr();
				}
			}

			setState(586);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Array_stepContext extends ParserRuleContext {
		public Array_filter_stepContext array_filter_step() {
			return getRuleContext(Array_filter_stepContext.class,0);
		}
		public Array_slice_stepContext array_slice_step() {
			return getRuleContext(Array_slice_stepContext.class,0);
		}
		public Array_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArray_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArray_step(this);
		}
	}

	public final Array_stepContext array_step() throws RecognitionException {
		Array_stepContext _localctx = new Array_stepContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_array_step);
		try {
			setState(590);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,43,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(588);
				array_filter_step();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(589);
				array_slice_step();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Array_slice_stepContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public TerminalNode COLON() { return getToken(KVQLParser.COLON, 0); }
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public Array_slice_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_slice_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArray_slice_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArray_slice_step(this);
		}
	}

	public final Array_slice_stepContext array_slice_step() throws RecognitionException {
		Array_slice_stepContext _localctx = new Array_slice_stepContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_array_slice_step);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(592);
			match(LBRACK);
			setState(594);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)) | (1L << (LP - 64)) | (1L << (LBRACK - 64)) | (1L << (LBRACE - 64)) | (1L << (DOLLAR - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (PLUS - 128)) | (1L << (MINUS - 128)) | (1L << (NULL - 128)) | (1L << (FALSE - 128)) | (1L << (TRUE - 128)) | (1L << (INT - 128)) | (1L << (FLOAT - 128)) | (1L << (DSTRING - 128)) | (1L << (STRING - 128)) | (1L << (ID - 128)) | (1L << (BAD_ID - 128)))) != 0)) {
				{
				setState(593);
				expr();
				}
			}

			setState(596);
			match(COLON);
			setState(598);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)) | (1L << (LP - 64)) | (1L << (LBRACK - 64)) | (1L << (LBRACE - 64)) | (1L << (DOLLAR - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (PLUS - 128)) | (1L << (MINUS - 128)) | (1L << (NULL - 128)) | (1L << (FALSE - 128)) | (1L << (TRUE - 128)) | (1L << (INT - 128)) | (1L << (FLOAT - 128)) | (1L << (DSTRING - 128)) | (1L << (STRING - 128)) | (1L << (ID - 128)) | (1L << (BAD_ID - 128)))) != 0)) {
				{
				setState(597);
				expr();
				}
			}

			setState(600);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Array_filter_stepContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public Array_filter_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_filter_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArray_filter_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArray_filter_step(this);
		}
	}

	public final Array_filter_stepContext array_filter_step() throws RecognitionException {
		Array_filter_stepContext _localctx = new Array_filter_stepContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_array_filter_step);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(602);
			match(LBRACK);
			setState(604);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)) | (1L << (LP - 64)) | (1L << (LBRACK - 64)) | (1L << (LBRACE - 64)) | (1L << (DOLLAR - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (PLUS - 128)) | (1L << (MINUS - 128)) | (1L << (NULL - 128)) | (1L << (FALSE - 128)) | (1L << (TRUE - 128)) | (1L << (INT - 128)) | (1L << (FLOAT - 128)) | (1L << (DSTRING - 128)) | (1L << (STRING - 128)) | (1L << (ID - 128)) | (1L << (BAD_ID - 128)))) != 0)) {
				{
				setState(603);
				expr();
				}
			}

			setState(606);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Primary_exprContext extends ParserRuleContext {
		public Const_exprContext const_expr() {
			return getRuleContext(Const_exprContext.class,0);
		}
		public Column_refContext column_ref() {
			return getRuleContext(Column_refContext.class,0);
		}
		public Var_refContext var_ref() {
			return getRuleContext(Var_refContext.class,0);
		}
		public Array_constructorContext array_constructor() {
			return getRuleContext(Array_constructorContext.class,0);
		}
		public Map_constructorContext map_constructor() {
			return getRuleContext(Map_constructorContext.class,0);
		}
		public Func_callContext func_call() {
			return getRuleContext(Func_callContext.class,0);
		}
		public Case_exprContext case_expr() {
			return getRuleContext(Case_exprContext.class,0);
		}
		public Cast_exprContext cast_expr() {
			return getRuleContext(Cast_exprContext.class,0);
		}
		public Parenthesized_exprContext parenthesized_expr() {
			return getRuleContext(Parenthesized_exprContext.class,0);
		}
		public Primary_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterPrimary_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitPrimary_expr(this);
		}
	}

	public final Primary_exprContext primary_expr() throws RecognitionException {
		Primary_exprContext _localctx = new Primary_exprContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_primary_expr);
		try {
			setState(617);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,47,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(608);
				const_expr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(609);
				column_ref();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(610);
				var_ref();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(611);
				array_constructor();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(612);
				map_constructor();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(613);
				func_call();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(614);
				case_expr();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(615);
				cast_expr();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(616);
				parenthesized_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Column_refContext extends ParserRuleContext {
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public TerminalNode DOT() { return getToken(KVQLParser.DOT, 0); }
		public Column_refContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_column_ref; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterColumn_ref(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitColumn_ref(this);
		}
	}

	public final Column_refContext column_ref() throws RecognitionException {
		Column_refContext _localctx = new Column_refContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_column_ref);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(619);
			id();
			setState(622);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
			case 1:
				{
				setState(620);
				match(DOT);
				setState(621);
				id();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Const_exprContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public TerminalNode FLOAT() { return getToken(KVQLParser.FLOAT, 0); }
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public TerminalNode TRUE() { return getToken(KVQLParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(KVQLParser.FALSE, 0); }
		public TerminalNode NULL() { return getToken(KVQLParser.NULL, 0); }
		public Const_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_const_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterConst_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitConst_expr(this);
		}
	}

	public final Const_exprContext const_expr() throws RecognitionException {
		Const_exprContext _localctx = new Const_exprContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_const_expr);
		try {
			setState(630);
			switch (_input.LA(1)) {
			case INT:
				enterOuterAlt(_localctx, 1);
				{
				setState(624);
				match(INT);
				}
				break;
			case FLOAT:
				enterOuterAlt(_localctx, 2);
				{
				setState(625);
				match(FLOAT);
				}
				break;
			case DSTRING:
			case STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(626);
				string();
				}
				break;
			case TRUE:
				enterOuterAlt(_localctx, 4);
				{
				setState(627);
				match(TRUE);
				}
				break;
			case FALSE:
				enterOuterAlt(_localctx, 5);
				{
				setState(628);
				match(FALSE);
				}
				break;
			case NULL:
				enterOuterAlt(_localctx, 6);
				{
				setState(629);
				match(NULL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Var_refContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(KVQLParser.DOLLAR, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Var_refContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_var_ref; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterVar_ref(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitVar_ref(this);
		}
	}

	public final Var_refContext var_ref() throws RecognitionException {
		Var_refContext _localctx = new Var_refContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_var_ref);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(632);
			match(DOLLAR);
			setState(634);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,50,_ctx) ) {
			case 1:
				{
				setState(633);
				id();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Array_constructorContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Array_constructorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_constructor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArray_constructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArray_constructor(this);
		}
	}

	public final Array_constructorContext array_constructor() throws RecognitionException {
		Array_constructorContext _localctx = new Array_constructorContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_array_constructor);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(636);
			match(LBRACK);
			setState(638);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)) | (1L << (LP - 64)) | (1L << (LBRACK - 64)) | (1L << (LBRACE - 64)) | (1L << (DOLLAR - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (PLUS - 128)) | (1L << (MINUS - 128)) | (1L << (NULL - 128)) | (1L << (FALSE - 128)) | (1L << (TRUE - 128)) | (1L << (INT - 128)) | (1L << (FLOAT - 128)) | (1L << (DSTRING - 128)) | (1L << (STRING - 128)) | (1L << (ID - 128)) | (1L << (BAD_ID - 128)))) != 0)) {
				{
				setState(637);
				expr();
				}
			}

			setState(644);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(640);
				match(COMMA);
				setState(641);
				expr();
				}
				}
				setState(646);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(647);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Map_constructorContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(KVQLParser.LBRACE, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COLON() { return getTokens(KVQLParser.COLON); }
		public TerminalNode COLON(int i) {
			return getToken(KVQLParser.COLON, i);
		}
		public TerminalNode RBRACE() { return getToken(KVQLParser.RBRACE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Map_constructorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_constructor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMap_constructor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMap_constructor(this);
		}
	}

	public final Map_constructorContext map_constructor() throws RecognitionException {
		Map_constructorContext _localctx = new Map_constructorContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_map_constructor);
		int _la;
		try {
			setState(667);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(649);
				match(LBRACE);
				setState(650);
				expr();
				setState(651);
				match(COLON);
				setState(652);
				expr();
				setState(660);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(653);
					match(COMMA);
					setState(654);
					expr();
					setState(655);
					match(COLON);
					setState(656);
					expr();
					}
					}
					setState(662);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(663);
				match(RBRACE);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(665);
				match(LBRACE);
				setState(666);
				match(RBRACE);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Func_callContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Func_callContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_func_call; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFunc_call(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFunc_call(this);
		}
	}

	public final Func_callContext func_call() throws RecognitionException {
		Func_callContext _localctx = new Func_callContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_func_call);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(669);
			id();
			setState(670);
			match(LP);
			setState(679);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)) | (1L << (LP - 64)) | (1L << (LBRACK - 64)) | (1L << (LBRACE - 64)) | (1L << (DOLLAR - 64)))) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & ((1L << (PLUS - 128)) | (1L << (MINUS - 128)) | (1L << (NULL - 128)) | (1L << (FALSE - 128)) | (1L << (TRUE - 128)) | (1L << (INT - 128)) | (1L << (FLOAT - 128)) | (1L << (DSTRING - 128)) | (1L << (STRING - 128)) | (1L << (ID - 128)) | (1L << (BAD_ID - 128)))) != 0)) {
				{
				setState(671);
				expr();
				setState(676);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(672);
					match(COMMA);
					setState(673);
					expr();
					}
					}
					setState(678);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(681);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Case_exprContext extends ParserRuleContext {
		public TerminalNode CASE() { return getToken(KVQLParser.CASE, 0); }
		public List<TerminalNode> WHEN() { return getTokens(KVQLParser.WHEN); }
		public TerminalNode WHEN(int i) {
			return getToken(KVQLParser.WHEN, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> THEN() { return getTokens(KVQLParser.THEN); }
		public TerminalNode THEN(int i) {
			return getToken(KVQLParser.THEN, i);
		}
		public TerminalNode END() { return getToken(KVQLParser.END, 0); }
		public TerminalNode ELSE() { return getToken(KVQLParser.ELSE, 0); }
		public Case_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_case_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCase_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCase_expr(this);
		}
	}

	public final Case_exprContext case_expr() throws RecognitionException {
		Case_exprContext _localctx = new Case_exprContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_case_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(683);
			match(CASE);
			setState(684);
			match(WHEN);
			setState(685);
			expr();
			setState(686);
			match(THEN);
			setState(687);
			expr();
			setState(695);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WHEN) {
				{
				{
				setState(688);
				match(WHEN);
				setState(689);
				expr();
				setState(690);
				match(THEN);
				setState(691);
				expr();
				}
				}
				setState(697);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(700);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(698);
				match(ELSE);
				setState(699);
				expr();
				}
			}

			setState(702);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Cast_exprContext extends ParserRuleContext {
		public TerminalNode CAST() { return getToken(KVQLParser.CAST, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode AS() { return getToken(KVQLParser.AS, 0); }
		public Quantified_type_defContext quantified_type_def() {
			return getRuleContext(Quantified_type_defContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Cast_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cast_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCast_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCast_expr(this);
		}
	}

	public final Cast_exprContext cast_expr() throws RecognitionException {
		Cast_exprContext _localctx = new Cast_exprContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_cast_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(704);
			match(CAST);
			setState(705);
			match(LP);
			setState(706);
			expr();
			setState(707);
			match(AS);
			setState(708);
			quantified_type_def();
			setState(709);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Parenthesized_exprContext extends ParserRuleContext {
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Parenthesized_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parenthesized_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterParenthesized_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitParenthesized_expr(this);
		}
	}

	public final Parenthesized_exprContext parenthesized_expr() throws RecognitionException {
		Parenthesized_exprContext _localctx = new Parenthesized_exprContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_parenthesized_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(711);
			match(LP);
			setState(712);
			expr();
			setState(713);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Quantified_type_defContext extends ParserRuleContext {
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public TerminalNode STAR() { return getToken(KVQLParser.STAR, 0); }
		public TerminalNode PLUS() { return getToken(KVQLParser.PLUS, 0); }
		public TerminalNode QUESTION_MARK() { return getToken(KVQLParser.QUESTION_MARK, 0); }
		public Quantified_type_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_quantified_type_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterQuantified_type_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitQuantified_type_def(this);
		}
	}

	public final Quantified_type_defContext quantified_type_def() throws RecognitionException {
		Quantified_type_defContext _localctx = new Quantified_type_defContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_quantified_type_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(715);
			type_def();
			setState(717);
			_la = _input.LA(1);
			if (((((_la - 112)) & ~0x3f) == 0 && ((1L << (_la - 112)) & ((1L << (STAR - 112)) | (1L << (QUESTION_MARK - 112)) | (1L << (PLUS - 112)))) != 0)) {
				{
				setState(716);
				_la = _input.LA(1);
				if ( !(((((_la - 112)) & ~0x3f) == 0 && ((1L << (_la - 112)) & ((1L << (STAR - 112)) | (1L << (QUESTION_MARK - 112)) | (1L << (PLUS - 112)))) != 0)) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Type_defContext extends ParserRuleContext {
		public Type_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_def; }
	 
		public Type_defContext() { }
		public void copyFrom(Type_defContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class EnumContext extends Type_defContext {
		public Enum_defContext enum_def() {
			return getRuleContext(Enum_defContext.class,0);
		}
		public EnumContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterEnum(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitEnum(this);
		}
	}
	public static class AnyAtomicContext extends Type_defContext {
		public AnyAtomic_defContext anyAtomic_def() {
			return getRuleContext(AnyAtomic_defContext.class,0);
		}
		public AnyAtomicContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnyAtomic(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnyAtomic(this);
		}
	}
	public static class AnyJsonAtomicContext extends Type_defContext {
		public AnyJsonAtomic_defContext anyJsonAtomic_def() {
			return getRuleContext(AnyJsonAtomic_defContext.class,0);
		}
		public AnyJsonAtomicContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnyJsonAtomic(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnyJsonAtomic(this);
		}
	}
	public static class AnyRecordContext extends Type_defContext {
		public AnyRecord_defContext anyRecord_def() {
			return getRuleContext(AnyRecord_defContext.class,0);
		}
		public AnyRecordContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnyRecord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnyRecord(this);
		}
	}
	public static class JSONContext extends Type_defContext {
		public Json_defContext json_def() {
			return getRuleContext(Json_defContext.class,0);
		}
		public JSONContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJSON(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJSON(this);
		}
	}
	public static class StringTContext extends Type_defContext {
		public String_defContext string_def() {
			return getRuleContext(String_defContext.class,0);
		}
		public StringTContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterStringT(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitStringT(this);
		}
	}
	public static class TimestampContext extends Type_defContext {
		public Timestamp_defContext timestamp_def() {
			return getRuleContext(Timestamp_defContext.class,0);
		}
		public TimestampContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterTimestamp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitTimestamp(this);
		}
	}
	public static class AnyContext extends Type_defContext {
		public Any_defContext any_def() {
			return getRuleContext(Any_defContext.class,0);
		}
		public AnyContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAny(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAny(this);
		}
	}
	public static class IntContext extends Type_defContext {
		public Integer_defContext integer_def() {
			return getRuleContext(Integer_defContext.class,0);
		}
		public IntContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterInt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitInt(this);
		}
	}
	public static class ArrayContext extends Type_defContext {
		public Array_defContext array_def() {
			return getRuleContext(Array_defContext.class,0);
		}
		public ArrayContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArray(this);
		}
	}
	public static class FloatContext extends Type_defContext {
		public Float_defContext float_def() {
			return getRuleContext(Float_defContext.class,0);
		}
		public FloatContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFloat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFloat(this);
		}
	}
	public static class RecordContext extends Type_defContext {
		public Record_defContext record_def() {
			return getRuleContext(Record_defContext.class,0);
		}
		public RecordContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterRecord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitRecord(this);
		}
	}
	public static class BinaryContext extends Type_defContext {
		public Binary_defContext binary_def() {
			return getRuleContext(Binary_defContext.class,0);
		}
		public BinaryContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterBinary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitBinary(this);
		}
	}
	public static class BooleanContext extends Type_defContext {
		public Boolean_defContext boolean_def() {
			return getRuleContext(Boolean_defContext.class,0);
		}
		public BooleanContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterBoolean(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitBoolean(this);
		}
	}
	public static class MapContext extends Type_defContext {
		public Map_defContext map_def() {
			return getRuleContext(Map_defContext.class,0);
		}
		public MapContext(Type_defContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMap(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMap(this);
		}
	}

	public final Type_defContext type_def() throws RecognitionException {
		Type_defContext _localctx = new Type_defContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_type_def);
		try {
			setState(734);
			switch (_input.LA(1)) {
			case BINARY_T:
				_localctx = new BinaryContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(719);
				binary_def();
				}
				break;
			case ARRAY_T:
				_localctx = new ArrayContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(720);
				array_def();
				}
				break;
			case BOOLEAN_T:
				_localctx = new BooleanContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(721);
				boolean_def();
				}
				break;
			case ENUM_T:
				_localctx = new EnumContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(722);
				enum_def();
				}
				break;
			case DOUBLE_T:
			case FLOAT_T:
			case NUMBER_T:
				_localctx = new FloatContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(723);
				float_def();
				}
				break;
			case INTEGER_T:
			case LONG_T:
				_localctx = new IntContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(724);
				integer_def();
				}
				break;
			case JSON:
				_localctx = new JSONContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(725);
				json_def();
				}
				break;
			case MAP_T:
				_localctx = new MapContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(726);
				map_def();
				}
				break;
			case RECORD_T:
				_localctx = new RecordContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(727);
				record_def();
				}
				break;
			case STRING_T:
				_localctx = new StringTContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(728);
				string_def();
				}
				break;
			case TIMESTAMP_T:
				_localctx = new TimestampContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(729);
				timestamp_def();
				}
				break;
			case ANY_T:
				_localctx = new AnyContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(730);
				any_def();
				}
				break;
			case ANYATOMIC_T:
				_localctx = new AnyAtomicContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(731);
				anyAtomic_def();
				}
				break;
			case ANYJSONATOMIC_T:
				_localctx = new AnyJsonAtomicContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(732);
				anyJsonAtomic_def();
				}
				break;
			case ANYRECORD_T:
				_localctx = new AnyRecordContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(733);
				anyRecord_def();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Record_defContext extends ParserRuleContext {
		public TerminalNode RECORD_T() { return getToken(KVQLParser.RECORD_T, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public List<Field_defContext> field_def() {
			return getRuleContexts(Field_defContext.class);
		}
		public Field_defContext field_def(int i) {
			return getRuleContext(Field_defContext.class,i);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Record_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_record_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterRecord_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitRecord_def(this);
		}
	}

	public final Record_defContext record_def() throws RecognitionException {
		Record_defContext _localctx = new Record_defContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_record_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(736);
			match(RECORD_T);
			setState(737);
			match(LP);
			setState(738);
			field_def();
			setState(743);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(739);
				match(COMMA);
				setState(740);
				field_def();
				}
				}
				setState(745);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(746);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Field_defContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public Default_defContext default_def() {
			return getRuleContext(Default_defContext.class,0);
		}
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public Field_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterField_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitField_def(this);
		}
	}

	public final Field_defContext field_def() throws RecognitionException {
		Field_defContext _localctx = new Field_defContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_field_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(748);
			id();
			setState(749);
			type_def();
			setState(751);
			_la = _input.LA(1);
			if (_la==DEFAULT || _la==NOT) {
				{
				setState(750);
				default_def();
				}
			}

			setState(754);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(753);
				comment();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Default_defContext extends ParserRuleContext {
		public Default_valueContext default_value() {
			return getRuleContext(Default_valueContext.class,0);
		}
		public Not_nullContext not_null() {
			return getRuleContext(Not_nullContext.class,0);
		}
		public Default_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_default_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDefault_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDefault_def(this);
		}
	}

	public final Default_defContext default_def() throws RecognitionException {
		Default_defContext _localctx = new Default_defContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_default_def);
		int _la;
		try {
			setState(764);
			switch (_input.LA(1)) {
			case DEFAULT:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(756);
				default_value();
				setState(758);
				_la = _input.LA(1);
				if (_la==NOT) {
					{
					setState(757);
					not_null();
					}
				}

				}
				}
				break;
			case NOT:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(760);
				not_null();
				setState(762);
				_la = _input.LA(1);
				if (_la==DEFAULT) {
					{
					setState(761);
					default_value();
					}
				}

				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Default_valueContext extends ParserRuleContext {
		public TerminalNode DEFAULT() { return getToken(KVQLParser.DEFAULT, 0); }
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public TerminalNode TRUE() { return getToken(KVQLParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(KVQLParser.FALSE, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Default_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_default_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDefault_value(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDefault_value(this);
		}
	}

	public final Default_valueContext default_value() throws RecognitionException {
		Default_valueContext _localctx = new Default_valueContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_default_value);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(766);
			match(DEFAULT);
			setState(772);
			switch (_input.LA(1)) {
			case MINUS:
			case INT:
			case FLOAT:
				{
				setState(767);
				number();
				}
				break;
			case DSTRING:
			case STRING:
				{
				setState(768);
				string();
				}
				break;
			case TRUE:
				{
				setState(769);
				match(TRUE);
				}
				break;
			case FALSE:
				{
				setState(770);
				match(FALSE);
				}
				break;
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case ID:
			case BAD_ID:
				{
				setState(771);
				id();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Not_nullContext extends ParserRuleContext {
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public TerminalNode NULL() { return getToken(KVQLParser.NULL, 0); }
		public Not_nullContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_not_null; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterNot_null(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitNot_null(this);
		}
	}

	public final Not_nullContext not_null() throws RecognitionException {
		Not_nullContext _localctx = new Not_nullContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_not_null);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(774);
			match(NOT);
			setState(775);
			match(NULL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Map_defContext extends ParserRuleContext {
		public TerminalNode MAP_T() { return getToken(KVQLParser.MAP_T, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Map_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterMap_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitMap_def(this);
		}
	}

	public final Map_defContext map_def() throws RecognitionException {
		Map_defContext _localctx = new Map_defContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_map_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(777);
			match(MAP_T);
			setState(778);
			match(LP);
			setState(779);
			type_def();
			setState(780);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Array_defContext extends ParserRuleContext {
		public TerminalNode ARRAY_T() { return getToken(KVQLParser.ARRAY_T, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Array_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArray_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArray_def(this);
		}
	}

	public final Array_defContext array_def() throws RecognitionException {
		Array_defContext _localctx = new Array_defContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_array_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(782);
			match(ARRAY_T);
			setState(783);
			match(LP);
			setState(784);
			type_def();
			setState(785);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Integer_defContext extends ParserRuleContext {
		public TerminalNode INTEGER_T() { return getToken(KVQLParser.INTEGER_T, 0); }
		public TerminalNode LONG_T() { return getToken(KVQLParser.LONG_T, 0); }
		public Integer_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_integer_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterInteger_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitInteger_def(this);
		}
	}

	public final Integer_defContext integer_def() throws RecognitionException {
		Integer_defContext _localctx = new Integer_defContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_integer_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(787);
			_la = _input.LA(1);
			if ( !(_la==INTEGER_T || _la==LONG_T) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Json_defContext extends ParserRuleContext {
		public TerminalNode JSON() { return getToken(KVQLParser.JSON, 0); }
		public Json_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_json_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJson_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJson_def(this);
		}
	}

	public final Json_defContext json_def() throws RecognitionException {
		Json_defContext _localctx = new Json_defContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_json_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(789);
			match(JSON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Float_defContext extends ParserRuleContext {
		public TerminalNode FLOAT_T() { return getToken(KVQLParser.FLOAT_T, 0); }
		public TerminalNode DOUBLE_T() { return getToken(KVQLParser.DOUBLE_T, 0); }
		public TerminalNode NUMBER_T() { return getToken(KVQLParser.NUMBER_T, 0); }
		public Float_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_float_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFloat_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFloat_def(this);
		}
	}

	public final Float_defContext float_def() throws RecognitionException {
		Float_defContext _localctx = new Float_defContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_float_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(791);
			_la = _input.LA(1);
			if ( !(((((_la - 89)) & ~0x3f) == 0 && ((1L << (_la - 89)) & ((1L << (DOUBLE_T - 89)) | (1L << (FLOAT_T - 89)) | (1L << (NUMBER_T - 89)))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class String_defContext extends ParserRuleContext {
		public TerminalNode STRING_T() { return getToken(KVQLParser.STRING_T, 0); }
		public String_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterString_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitString_def(this);
		}
	}

	public final String_defContext string_def() throws RecognitionException {
		String_defContext _localctx = new String_defContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_string_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(793);
			match(STRING_T);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Enum_defContext extends ParserRuleContext {
		public TerminalNode ENUM_T() { return getToken(KVQLParser.ENUM_T, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Id_listContext id_list() {
			return getRuleContext(Id_listContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Enum_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterEnum_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitEnum_def(this);
		}
	}

	public final Enum_defContext enum_def() throws RecognitionException {
		Enum_defContext _localctx = new Enum_defContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_enum_def);
		try {
			setState(805);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(795);
				match(ENUM_T);
				setState(796);
				match(LP);
				setState(797);
				id_list();
				setState(798);
				match(RP);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(800);
				match(ENUM_T);
				setState(801);
				match(LP);
				setState(802);
				id_list();
				 notifyErrorListeners("Missing closing ')'"); 
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Boolean_defContext extends ParserRuleContext {
		public TerminalNode BOOLEAN_T() { return getToken(KVQLParser.BOOLEAN_T, 0); }
		public Boolean_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boolean_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterBoolean_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitBoolean_def(this);
		}
	}

	public final Boolean_defContext boolean_def() throws RecognitionException {
		Boolean_defContext _localctx = new Boolean_defContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_boolean_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(807);
			match(BOOLEAN_T);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Binary_defContext extends ParserRuleContext {
		public TerminalNode BINARY_T() { return getToken(KVQLParser.BINARY_T, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Binary_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binary_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterBinary_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitBinary_def(this);
		}
	}

	public final Binary_defContext binary_def() throws RecognitionException {
		Binary_defContext _localctx = new Binary_defContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_binary_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(809);
			match(BINARY_T);
			setState(813);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(810);
				match(LP);
				setState(811);
				match(INT);
				setState(812);
				match(RP);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Timestamp_defContext extends ParserRuleContext {
		public TerminalNode TIMESTAMP_T() { return getToken(KVQLParser.TIMESTAMP_T, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Timestamp_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_timestamp_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterTimestamp_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitTimestamp_def(this);
		}
	}

	public final Timestamp_defContext timestamp_def() throws RecognitionException {
		Timestamp_defContext _localctx = new Timestamp_defContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_timestamp_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(815);
			match(TIMESTAMP_T);
			setState(819);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(816);
				match(LP);
				setState(817);
				match(INT);
				setState(818);
				match(RP);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Any_defContext extends ParserRuleContext {
		public TerminalNode ANY_T() { return getToken(KVQLParser.ANY_T, 0); }
		public Any_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_any_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAny_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAny_def(this);
		}
	}

	public final Any_defContext any_def() throws RecognitionException {
		Any_defContext _localctx = new Any_defContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_any_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(821);
			match(ANY_T);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnyAtomic_defContext extends ParserRuleContext {
		public TerminalNode ANYATOMIC_T() { return getToken(KVQLParser.ANYATOMIC_T, 0); }
		public AnyAtomic_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anyAtomic_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnyAtomic_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnyAtomic_def(this);
		}
	}

	public final AnyAtomic_defContext anyAtomic_def() throws RecognitionException {
		AnyAtomic_defContext _localctx = new AnyAtomic_defContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_anyAtomic_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(823);
			match(ANYATOMIC_T);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnyJsonAtomic_defContext extends ParserRuleContext {
		public TerminalNode ANYJSONATOMIC_T() { return getToken(KVQLParser.ANYJSONATOMIC_T, 0); }
		public AnyJsonAtomic_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anyJsonAtomic_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnyJsonAtomic_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnyJsonAtomic_def(this);
		}
	}

	public final AnyJsonAtomic_defContext anyJsonAtomic_def() throws RecognitionException {
		AnyJsonAtomic_defContext _localctx = new AnyJsonAtomic_defContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_anyJsonAtomic_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(825);
			match(ANYJSONATOMIC_T);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnyRecord_defContext extends ParserRuleContext {
		public TerminalNode ANYRECORD_T() { return getToken(KVQLParser.ANYRECORD_T, 0); }
		public AnyRecord_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_anyRecord_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAnyRecord_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAnyRecord_def(this);
		}
	}

	public final AnyRecord_defContext anyRecord_def() throws RecognitionException {
		AnyRecord_defContext _localctx = new AnyRecord_defContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_anyRecord_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(827);
			match(ANYRECORD_T);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Name_pathContext extends ParserRuleContext {
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(KVQLParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(KVQLParser.DOT, i);
		}
		public Name_pathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterName_path(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitName_path(this);
		}
	}

	public final Name_pathContext name_path() throws RecognitionException {
		Name_pathContext _localctx = new Name_pathContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_name_path);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(829);
			id();
			setState(834);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,71,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(830);
					match(DOT);
					setState(831);
					id();
					}
					} 
				}
				setState(836);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,71,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Create_table_statementContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(KVQLParser.CREATE, 0); }
		public TerminalNode TABLE() { return getToken(KVQLParser.TABLE, 0); }
		public Table_nameContext table_name() {
			return getRuleContext(Table_nameContext.class,0);
		}
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Table_defContext table_def() {
			return getRuleContext(Table_defContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public TerminalNode IF() { return getToken(KVQLParser.IF, 0); }
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public Ttl_defContext ttl_def() {
			return getRuleContext(Ttl_defContext.class,0);
		}
		public Create_table_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_table_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCreate_table_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCreate_table_statement(this);
		}
	}

	public final Create_table_statementContext create_table_statement() throws RecognitionException {
		Create_table_statementContext _localctx = new Create_table_statementContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_create_table_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(837);
			match(CREATE);
			setState(838);
			match(TABLE);
			setState(842);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,72,_ctx) ) {
			case 1:
				{
				setState(839);
				match(IF);
				setState(840);
				match(NOT);
				setState(841);
				match(EXISTS);
				}
				break;
			}
			setState(844);
			table_name();
			setState(846);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(845);
				comment();
				}
			}

			setState(848);
			match(LP);
			setState(849);
			table_def();
			setState(850);
			match(RP);
			setState(852);
			_la = _input.LA(1);
			if (_la==USING) {
				{
				setState(851);
				ttl_def();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Table_nameContext extends ParserRuleContext {
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public Table_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_table_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterTable_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitTable_name(this);
		}
	}

	public final Table_nameContext table_name() throws RecognitionException {
		Table_nameContext _localctx = new Table_nameContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_table_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(854);
			name_path();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Table_defContext extends ParserRuleContext {
		public List<Field_defContext> field_def() {
			return getRuleContexts(Field_defContext.class);
		}
		public Field_defContext field_def(int i) {
			return getRuleContext(Field_defContext.class,i);
		}
		public List<Key_defContext> key_def() {
			return getRuleContexts(Key_defContext.class);
		}
		public Key_defContext key_def(int i) {
			return getRuleContext(Key_defContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Table_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_table_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterTable_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitTable_def(this);
		}
	}

	public final Table_defContext table_def() throws RecognitionException {
		Table_defContext _localctx = new Table_defContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_table_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(858);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,75,_ctx) ) {
			case 1:
				{
				setState(856);
				field_def();
				}
				break;
			case 2:
				{
				setState(857);
				key_def();
				}
				break;
			}
			setState(867);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(860);
				match(COMMA);
				setState(863);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,76,_ctx) ) {
				case 1:
					{
					setState(861);
					field_def();
					}
					break;
				case 2:
					{
					setState(862);
					key_def();
					}
					break;
				}
				}
				}
				setState(869);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Key_defContext extends ParserRuleContext {
		public TerminalNode PRIMARY() { return getToken(KVQLParser.PRIMARY, 0); }
		public TerminalNode KEY() { return getToken(KVQLParser.KEY, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Shard_key_defContext shard_key_def() {
			return getRuleContext(Shard_key_defContext.class,0);
		}
		public Id_list_with_sizeContext id_list_with_size() {
			return getRuleContext(Id_list_with_sizeContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(KVQLParser.COMMA, 0); }
		public Key_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_key_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterKey_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitKey_def(this);
		}
	}

	public final Key_defContext key_def() throws RecognitionException {
		Key_defContext _localctx = new Key_defContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_key_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(870);
			match(PRIMARY);
			setState(871);
			match(KEY);
			setState(872);
			match(LP);
			setState(877);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
			case 1:
				{
				setState(873);
				shard_key_def();
				setState(875);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(874);
					match(COMMA);
					}
				}

				}
				break;
			}
			setState(880);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)))) != 0) || _la==ID || _la==BAD_ID) {
				{
				setState(879);
				id_list_with_size();
				}
			}

			setState(882);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Shard_key_defContext extends ParserRuleContext {
		public TerminalNode SHARD() { return getToken(KVQLParser.SHARD, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Id_list_with_sizeContext id_list_with_size() {
			return getRuleContext(Id_list_with_sizeContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Shard_key_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shard_key_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterShard_key_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitShard_key_def(this);
		}
	}

	public final Shard_key_defContext shard_key_def() throws RecognitionException {
		Shard_key_defContext _localctx = new Shard_key_defContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_shard_key_def);
		try {
			setState(893);
			switch (_input.LA(1)) {
			case SHARD:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(884);
				match(SHARD);
				setState(885);
				match(LP);
				setState(886);
				id_list_with_size();
				setState(887);
				match(RP);
				}
				}
				break;
			case LP:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(889);
				match(LP);
				setState(890);
				id_list_with_size();
				 notifyErrorListeners("Missing closing ')'"); 
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Id_list_with_sizeContext extends ParserRuleContext {
		public List<Id_with_sizeContext> id_with_size() {
			return getRuleContexts(Id_with_sizeContext.class);
		}
		public Id_with_sizeContext id_with_size(int i) {
			return getRuleContext(Id_with_sizeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Id_list_with_sizeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id_list_with_size; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterId_list_with_size(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitId_list_with_size(this);
		}
	}

	public final Id_list_with_sizeContext id_list_with_size() throws RecognitionException {
		Id_list_with_sizeContext _localctx = new Id_list_with_sizeContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_id_list_with_size);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(895);
			id_with_size();
			setState(900);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(896);
					match(COMMA);
					setState(897);
					id_with_size();
					}
					} 
				}
				setState(902);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Id_with_sizeContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Storage_sizeContext storage_size() {
			return getRuleContext(Storage_sizeContext.class,0);
		}
		public Id_with_sizeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id_with_size; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterId_with_size(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitId_with_size(this);
		}
	}

	public final Id_with_sizeContext id_with_size() throws RecognitionException {
		Id_with_sizeContext _localctx = new Id_with_sizeContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_id_with_size);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(903);
			id();
			setState(905);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(904);
				storage_size();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Storage_sizeContext extends ParserRuleContext {
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Storage_sizeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_storage_size; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterStorage_size(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitStorage_size(this);
		}
	}

	public final Storage_sizeContext storage_size() throws RecognitionException {
		Storage_sizeContext _localctx = new Storage_sizeContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_storage_size);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(907);
			match(LP);
			setState(908);
			match(INT);
			setState(909);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Ttl_defContext extends ParserRuleContext {
		public TerminalNode USING() { return getToken(KVQLParser.USING, 0); }
		public TerminalNode TTL() { return getToken(KVQLParser.TTL, 0); }
		public DurationContext duration() {
			return getRuleContext(DurationContext.class,0);
		}
		public Ttl_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ttl_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterTtl_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitTtl_def(this);
		}
	}

	public final Ttl_defContext ttl_def() throws RecognitionException {
		Ttl_defContext _localctx = new Ttl_defContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_ttl_def);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(911);
			match(USING);
			setState(912);
			match(TTL);
			setState(913);
			duration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Alter_table_statementContext extends ParserRuleContext {
		public TerminalNode ALTER() { return getToken(KVQLParser.ALTER, 0); }
		public TerminalNode TABLE() { return getToken(KVQLParser.TABLE, 0); }
		public Table_nameContext table_name() {
			return getRuleContext(Table_nameContext.class,0);
		}
		public Alter_defContext alter_def() {
			return getRuleContext(Alter_defContext.class,0);
		}
		public Alter_table_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alter_table_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAlter_table_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAlter_table_statement(this);
		}
	}

	public final Alter_table_statementContext alter_table_statement() throws RecognitionException {
		Alter_table_statementContext _localctx = new Alter_table_statementContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_alter_table_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(915);
			match(ALTER);
			setState(916);
			match(TABLE);
			setState(917);
			table_name();
			setState(918);
			alter_def();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Alter_defContext extends ParserRuleContext {
		public Alter_field_statementsContext alter_field_statements() {
			return getRuleContext(Alter_field_statementsContext.class,0);
		}
		public Ttl_defContext ttl_def() {
			return getRuleContext(Ttl_defContext.class,0);
		}
		public Alter_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alter_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAlter_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAlter_def(this);
		}
	}

	public final Alter_defContext alter_def() throws RecognitionException {
		Alter_defContext _localctx = new Alter_defContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_alter_def);
		try {
			setState(922);
			switch (_input.LA(1)) {
			case LP:
				enterOuterAlt(_localctx, 1);
				{
				setState(920);
				alter_field_statements();
				}
				break;
			case USING:
				enterOuterAlt(_localctx, 2);
				{
				setState(921);
				ttl_def();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Alter_field_statementsContext extends ParserRuleContext {
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public List<Add_field_statementContext> add_field_statement() {
			return getRuleContexts(Add_field_statementContext.class);
		}
		public Add_field_statementContext add_field_statement(int i) {
			return getRuleContext(Add_field_statementContext.class,i);
		}
		public List<Drop_field_statementContext> drop_field_statement() {
			return getRuleContexts(Drop_field_statementContext.class);
		}
		public Drop_field_statementContext drop_field_statement(int i) {
			return getRuleContext(Drop_field_statementContext.class,i);
		}
		public List<Modify_field_statementContext> modify_field_statement() {
			return getRuleContexts(Modify_field_statementContext.class);
		}
		public Modify_field_statementContext modify_field_statement(int i) {
			return getRuleContext(Modify_field_statementContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Alter_field_statementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alter_field_statements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAlter_field_statements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAlter_field_statements(this);
		}
	}

	public final Alter_field_statementsContext alter_field_statements() throws RecognitionException {
		Alter_field_statementsContext _localctx = new Alter_field_statementsContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_alter_field_statements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(924);
			match(LP);
			setState(928);
			switch (_input.LA(1)) {
			case ADD:
				{
				setState(925);
				add_field_statement();
				}
				break;
			case DROP:
				{
				setState(926);
				drop_field_statement();
				}
				break;
			case MODIFY:
				{
				setState(927);
				modify_field_statement();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(938);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(930);
				match(COMMA);
				setState(934);
				switch (_input.LA(1)) {
				case ADD:
					{
					setState(931);
					add_field_statement();
					}
					break;
				case DROP:
					{
					setState(932);
					drop_field_statement();
					}
					break;
				case MODIFY:
					{
					setState(933);
					modify_field_statement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				}
				setState(940);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(941);
			match(RP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Add_field_statementContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(KVQLParser.ADD, 0); }
		public Schema_pathContext schema_path() {
			return getRuleContext(Schema_pathContext.class,0);
		}
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public Default_defContext default_def() {
			return getRuleContext(Default_defContext.class,0);
		}
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public Add_field_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_add_field_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAdd_field_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAdd_field_statement(this);
		}
	}

	public final Add_field_statementContext add_field_statement() throws RecognitionException {
		Add_field_statementContext _localctx = new Add_field_statementContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_add_field_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(943);
			match(ADD);
			setState(944);
			schema_path();
			setState(945);
			type_def();
			setState(947);
			_la = _input.LA(1);
			if (_la==DEFAULT || _la==NOT) {
				{
				setState(946);
				default_def();
				}
			}

			setState(950);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(949);
				comment();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Drop_field_statementContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(KVQLParser.DROP, 0); }
		public Schema_pathContext schema_path() {
			return getRuleContext(Schema_pathContext.class,0);
		}
		public Drop_field_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_drop_field_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDrop_field_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDrop_field_statement(this);
		}
	}

	public final Drop_field_statementContext drop_field_statement() throws RecognitionException {
		Drop_field_statementContext _localctx = new Drop_field_statementContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_drop_field_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(952);
			match(DROP);
			setState(953);
			schema_path();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Modify_field_statementContext extends ParserRuleContext {
		public TerminalNode MODIFY() { return getToken(KVQLParser.MODIFY, 0); }
		public Schema_pathContext schema_path() {
			return getRuleContext(Schema_pathContext.class,0);
		}
		public Type_defContext type_def() {
			return getRuleContext(Type_defContext.class,0);
		}
		public Default_defContext default_def() {
			return getRuleContext(Default_defContext.class,0);
		}
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public Modify_field_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modify_field_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterModify_field_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitModify_field_statement(this);
		}
	}

	public final Modify_field_statementContext modify_field_statement() throws RecognitionException {
		Modify_field_statementContext _localctx = new Modify_field_statementContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_modify_field_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(955);
			match(MODIFY);
			setState(956);
			schema_path();
			setState(957);
			type_def();
			setState(959);
			_la = _input.LA(1);
			if (_la==DEFAULT || _la==NOT) {
				{
				setState(958);
				default_def();
				}
			}

			setState(962);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(961);
				comment();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Schema_pathContext extends ParserRuleContext {
		public Init_schema_path_stepContext init_schema_path_step() {
			return getRuleContext(Init_schema_path_stepContext.class,0);
		}
		public List<TerminalNode> DOT() { return getTokens(KVQLParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(KVQLParser.DOT, i);
		}
		public List<Schema_path_stepContext> schema_path_step() {
			return getRuleContexts(Schema_path_stepContext.class);
		}
		public Schema_path_stepContext schema_path_step(int i) {
			return getRuleContext(Schema_path_stepContext.class,i);
		}
		public Schema_pathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schema_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSchema_path(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSchema_path(this);
		}
	}

	public final Schema_pathContext schema_path() throws RecognitionException {
		Schema_pathContext _localctx = new Schema_pathContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_schema_path);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(964);
			init_schema_path_step();
			setState(969);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(965);
				match(DOT);
				setState(966);
				schema_path_step();
				}
				}
				setState(971);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Init_schema_path_stepContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TerminalNode> LBRACK() { return getTokens(KVQLParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(KVQLParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(KVQLParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(KVQLParser.RBRACK, i);
		}
		public Init_schema_path_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_init_schema_path_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterInit_schema_path_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitInit_schema_path_step(this);
		}
	}

	public final Init_schema_path_stepContext init_schema_path_step() throws RecognitionException {
		Init_schema_path_stepContext _localctx = new Init_schema_path_stepContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_init_schema_path_step);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(972);
			id();
			setState(977);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(973);
				match(LBRACK);
				setState(974);
				match(RBRACK);
				}
				}
				setState(979);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Schema_path_stepContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public List<TerminalNode> LBRACK() { return getTokens(KVQLParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(KVQLParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(KVQLParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(KVQLParser.RBRACK, i);
		}
		public TerminalNode VALUES() { return getToken(KVQLParser.VALUES, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Schema_path_stepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schema_path_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSchema_path_step(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSchema_path_step(this);
		}
	}

	public final Schema_path_stepContext schema_path_step() throws RecognitionException {
		Schema_path_stepContext _localctx = new Schema_path_stepContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_schema_path_step);
		int _la;
		try {
			setState(991);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,95,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(980);
				id();
				setState(985);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACK) {
					{
					{
					setState(981);
					match(LBRACK);
					setState(982);
					match(RBRACK);
					}
					}
					setState(987);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(988);
				match(VALUES);
				setState(989);
				match(LP);
				setState(990);
				match(RP);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Drop_table_statementContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(KVQLParser.DROP, 0); }
		public TerminalNode TABLE() { return getToken(KVQLParser.TABLE, 0); }
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public TerminalNode IF() { return getToken(KVQLParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public Drop_table_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_drop_table_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDrop_table_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDrop_table_statement(this);
		}
	}

	public final Drop_table_statementContext drop_table_statement() throws RecognitionException {
		Drop_table_statementContext _localctx = new Drop_table_statementContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_drop_table_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(993);
			match(DROP);
			setState(994);
			match(TABLE);
			setState(997);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,96,_ctx) ) {
			case 1:
				{
				setState(995);
				match(IF);
				setState(996);
				match(EXISTS);
				}
				break;
			}
			setState(999);
			name_path();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Create_index_statementContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(KVQLParser.CREATE, 0); }
		public TerminalNode INDEX() { return getToken(KVQLParser.INDEX, 0); }
		public Index_nameContext index_name() {
			return getRuleContext(Index_nameContext.class,0);
		}
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public Table_nameContext table_name() {
			return getRuleContext(Table_nameContext.class,0);
		}
		public TerminalNode IF() { return getToken(KVQLParser.IF, 0); }
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Index_path_listContext index_path_list() {
			return getRuleContext(Index_path_listContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Create_index_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_index_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCreate_index_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCreate_index_statement(this);
		}
	}

	public final Create_index_statementContext create_index_statement() throws RecognitionException {
		Create_index_statementContext _localctx = new Create_index_statementContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_create_index_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1001);
			match(CREATE);
			setState(1002);
			match(INDEX);
			setState(1006);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,97,_ctx) ) {
			case 1:
				{
				setState(1003);
				match(IF);
				setState(1004);
				match(NOT);
				setState(1005);
				match(EXISTS);
				}
				break;
			}
			setState(1008);
			index_name();
			setState(1009);
			match(ON);
			setState(1010);
			table_name();
			setState(1019);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
			case 1:
				{
				{
				setState(1011);
				match(LP);
				setState(1012);
				index_path_list();
				setState(1013);
				match(RP);
				}
				}
				break;
			case 2:
				{
				{
				setState(1015);
				match(LP);
				setState(1016);
				index_path_list();
				 notifyErrorListeners("Missing closing ')'"); 
				}
				}
				break;
			}
			setState(1022);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(1021);
				comment();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Index_nameContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Index_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterIndex_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitIndex_name(this);
		}
	}

	public final Index_nameContext index_name() throws RecognitionException {
		Index_nameContext _localctx = new Index_nameContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_index_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1024);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Index_path_listContext extends ParserRuleContext {
		public List<Index_pathContext> index_path() {
			return getRuleContexts(Index_pathContext.class);
		}
		public Index_pathContext index_path(int i) {
			return getRuleContext(Index_pathContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Index_path_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_path_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterIndex_path_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitIndex_path_list(this);
		}
	}

	public final Index_path_listContext index_path_list() throws RecognitionException {
		Index_path_listContext _localctx = new Index_path_listContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_index_path_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1026);
			index_path();
			setState(1031);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1027);
				match(COMMA);
				setState(1028);
				index_path();
				}
				}
				setState(1033);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Index_pathContext extends ParserRuleContext {
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public Keys_exprContext keys_expr() {
			return getRuleContext(Keys_exprContext.class,0);
		}
		public Values_exprContext values_expr() {
			return getRuleContext(Values_exprContext.class,0);
		}
		public Brackets_exprContext brackets_expr() {
			return getRuleContext(Brackets_exprContext.class,0);
		}
		public Index_pathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_index_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterIndex_path(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitIndex_path(this);
		}
	}

	public final Index_pathContext index_path() throws RecognitionException {
		Index_pathContext _localctx = new Index_pathContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_index_path);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1038);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,101,_ctx) ) {
			case 1:
				{
				setState(1034);
				name_path();
				}
				break;
			case 2:
				{
				setState(1035);
				keys_expr();
				}
				break;
			case 3:
				{
				setState(1036);
				values_expr();
				}
				break;
			case 4:
				{
				setState(1037);
				brackets_expr();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Keys_exprContext extends ParserRuleContext {
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public TerminalNode DOT() { return getToken(KVQLParser.DOT, 0); }
		public TerminalNode KEYS() { return getToken(KVQLParser.KEYS, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public TerminalNode KEYOF() { return getToken(KVQLParser.KEYOF, 0); }
		public Keys_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keys_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterKeys_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitKeys_expr(this);
		}
	}

	public final Keys_exprContext keys_expr() throws RecognitionException {
		Keys_exprContext _localctx = new Keys_exprContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_keys_expr);
		try {
			setState(1056);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,102,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1040);
				name_path();
				setState(1041);
				match(DOT);
				setState(1042);
				match(KEYS);
				setState(1043);
				match(LP);
				setState(1044);
				match(RP);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1046);
				match(KEYOF);
				setState(1047);
				match(LP);
				setState(1048);
				name_path();
				setState(1049);
				match(RP);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1051);
				match(KEYS);
				setState(1052);
				match(LP);
				setState(1053);
				name_path();
				setState(1054);
				match(RP);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Values_exprContext extends ParserRuleContext {
		public List<Name_pathContext> name_path() {
			return getRuleContexts(Name_pathContext.class);
		}
		public Name_pathContext name_path(int i) {
			return getRuleContext(Name_pathContext.class,i);
		}
		public TerminalNode DOT() { return getToken(KVQLParser.DOT, 0); }
		public TerminalNode VALUES() { return getToken(KVQLParser.VALUES, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public TerminalNode ELEMENTOF() { return getToken(KVQLParser.ELEMENTOF, 0); }
		public Values_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_values_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterValues_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitValues_expr(this);
		}
	}

	public final Values_exprContext values_expr() throws RecognitionException {
		Values_exprContext _localctx = new Values_exprContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_values_expr);
		int _la;
		try {
			setState(1082);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,106,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1058);
				name_path();
				setState(1059);
				match(DOT);
				setState(1060);
				match(VALUES);
				setState(1061);
				match(LP);
				setState(1062);
				match(RP);
				setState(1065);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(1063);
					match(DOT);
					setState(1064);
					name_path();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1076);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,104,_ctx) ) {
				case 1:
					{
					{
					setState(1067);
					name_path();
					setState(1068);
					match(LBRACK);
					setState(1069);
					match(RBRACK);
					}
					}
					break;
				case 2:
					{
					{
					setState(1071);
					match(ELEMENTOF);
					setState(1072);
					match(LP);
					setState(1073);
					name_path();
					setState(1074);
					match(RP);
					}
					}
					break;
				}
				setState(1080);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(1078);
					match(DOT);
					setState(1079);
					name_path();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Brackets_exprContext extends ParserRuleContext {
		public List<Name_pathContext> name_path() {
			return getRuleContexts(Name_pathContext.class);
		}
		public Name_pathContext name_path(int i) {
			return getRuleContext(Name_pathContext.class,i);
		}
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public Brackets_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_brackets_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterBrackets_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitBrackets_expr(this);
		}
	}

	public final Brackets_exprContext brackets_expr() throws RecognitionException {
		Brackets_exprContext _localctx = new Brackets_exprContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_brackets_expr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1084);
			name_path();
			setState(1085);
			match(LBRACK);
			setState(1086);
			match(RBRACK);
			setState(1089);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(1087);
				match(DOT);
				setState(1088);
				name_path();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Create_text_index_statementContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(KVQLParser.CREATE, 0); }
		public TerminalNode FULLTEXT() { return getToken(KVQLParser.FULLTEXT, 0); }
		public TerminalNode INDEX() { return getToken(KVQLParser.INDEX, 0); }
		public Index_nameContext index_name() {
			return getRuleContext(Index_nameContext.class,0);
		}
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public Table_nameContext table_name() {
			return getRuleContext(Table_nameContext.class,0);
		}
		public Fts_field_listContext fts_field_list() {
			return getRuleContext(Fts_field_listContext.class,0);
		}
		public TerminalNode IF() { return getToken(KVQLParser.IF, 0); }
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public Es_propertiesContext es_properties() {
			return getRuleContext(Es_propertiesContext.class,0);
		}
		public TerminalNode OVERRIDE() { return getToken(KVQLParser.OVERRIDE, 0); }
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
		public Create_text_index_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_text_index_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCreate_text_index_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCreate_text_index_statement(this);
		}
	}

	public final Create_text_index_statementContext create_text_index_statement() throws RecognitionException {
		Create_text_index_statementContext _localctx = new Create_text_index_statementContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_create_text_index_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1091);
			match(CREATE);
			setState(1092);
			match(FULLTEXT);
			setState(1093);
			match(INDEX);
			setState(1097);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,108,_ctx) ) {
			case 1:
				{
				setState(1094);
				match(IF);
				setState(1095);
				match(NOT);
				setState(1096);
				match(EXISTS);
				}
				break;
			}
			setState(1099);
			index_name();
			setState(1100);
			match(ON);
			setState(1101);
			table_name();
			setState(1102);
			fts_field_list();
			setState(1104);
			_la = _input.LA(1);
			if (_la==ES_SHARDS || _la==ES_REPLICAS) {
				{
				setState(1103);
				es_properties();
				}
			}

			setState(1107);
			_la = _input.LA(1);
			if (_la==OVERRIDE) {
				{
				setState(1106);
				match(OVERRIDE);
				}
			}

			setState(1110);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(1109);
				comment();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fts_field_listContext extends ParserRuleContext {
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Fts_path_listContext fts_path_list() {
			return getRuleContext(Fts_path_listContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Fts_field_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fts_field_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFts_field_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFts_field_list(this);
		}
	}

	public final Fts_field_listContext fts_field_list() throws RecognitionException {
		Fts_field_listContext _localctx = new Fts_field_listContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_fts_field_list);
		try {
			setState(1120);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1112);
				match(LP);
				setState(1113);
				fts_path_list();
				setState(1114);
				match(RP);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1116);
				match(LP);
				setState(1117);
				fts_path_list();
				notifyErrorListeners("Missing closing ')'");
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fts_path_listContext extends ParserRuleContext {
		public List<Fts_pathContext> fts_path() {
			return getRuleContexts(Fts_pathContext.class);
		}
		public Fts_pathContext fts_path(int i) {
			return getRuleContext(Fts_pathContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Fts_path_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fts_path_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFts_path_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFts_path_list(this);
		}
	}

	public final Fts_path_listContext fts_path_list() throws RecognitionException {
		Fts_path_listContext _localctx = new Fts_path_listContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_fts_path_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1122);
			fts_path();
			setState(1127);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1123);
				match(COMMA);
				setState(1124);
				fts_path();
				}
				}
				setState(1129);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fts_pathContext extends ParserRuleContext {
		public Index_pathContext index_path() {
			return getRuleContext(Index_pathContext.class,0);
		}
		public JsobjectContext jsobject() {
			return getRuleContext(JsobjectContext.class,0);
		}
		public Fts_pathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fts_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterFts_path(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitFts_path(this);
		}
	}

	public final Fts_pathContext fts_path() throws RecognitionException {
		Fts_pathContext _localctx = new Fts_pathContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_fts_path);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1130);
			index_path();
			setState(1132);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(1131);
				jsobject();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Es_propertiesContext extends ParserRuleContext {
		public List<Es_property_assignmentContext> es_property_assignment() {
			return getRuleContexts(Es_property_assignmentContext.class);
		}
		public Es_property_assignmentContext es_property_assignment(int i) {
			return getRuleContext(Es_property_assignmentContext.class,i);
		}
		public Es_propertiesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_es_properties; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterEs_properties(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitEs_properties(this);
		}
	}

	public final Es_propertiesContext es_properties() throws RecognitionException {
		Es_propertiesContext _localctx = new Es_propertiesContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_es_properties);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1134);
			es_property_assignment();
			setState(1138);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ES_SHARDS || _la==ES_REPLICAS) {
				{
				{
				setState(1135);
				es_property_assignment();
				}
				}
				setState(1140);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Es_property_assignmentContext extends ParserRuleContext {
		public TerminalNode ES_SHARDS() { return getToken(KVQLParser.ES_SHARDS, 0); }
		public TerminalNode EQ() { return getToken(KVQLParser.EQ, 0); }
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public TerminalNode ES_REPLICAS() { return getToken(KVQLParser.ES_REPLICAS, 0); }
		public Es_property_assignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_es_property_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterEs_property_assignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitEs_property_assignment(this);
		}
	}

	public final Es_property_assignmentContext es_property_assignment() throws RecognitionException {
		Es_property_assignmentContext _localctx = new Es_property_assignmentContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_es_property_assignment);
		try {
			setState(1147);
			switch (_input.LA(1)) {
			case ES_SHARDS:
				enterOuterAlt(_localctx, 1);
				{
				setState(1141);
				match(ES_SHARDS);
				setState(1142);
				match(EQ);
				setState(1143);
				match(INT);
				}
				break;
			case ES_REPLICAS:
				enterOuterAlt(_localctx, 2);
				{
				setState(1144);
				match(ES_REPLICAS);
				setState(1145);
				match(EQ);
				setState(1146);
				match(INT);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Drop_index_statementContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(KVQLParser.DROP, 0); }
		public TerminalNode INDEX() { return getToken(KVQLParser.INDEX, 0); }
		public Index_nameContext index_name() {
			return getRuleContext(Index_nameContext.class,0);
		}
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public TerminalNode IF() { return getToken(KVQLParser.IF, 0); }
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public TerminalNode OVERRIDE() { return getToken(KVQLParser.OVERRIDE, 0); }
		public Drop_index_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_drop_index_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDrop_index_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDrop_index_statement(this);
		}
	}

	public final Drop_index_statementContext drop_index_statement() throws RecognitionException {
		Drop_index_statementContext _localctx = new Drop_index_statementContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_drop_index_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1149);
			match(DROP);
			setState(1150);
			match(INDEX);
			setState(1153);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,117,_ctx) ) {
			case 1:
				{
				setState(1151);
				match(IF);
				setState(1152);
				match(EXISTS);
				}
				break;
			}
			setState(1155);
			index_name();
			setState(1156);
			match(ON);
			setState(1157);
			name_path();
			setState(1159);
			_la = _input.LA(1);
			if (_la==OVERRIDE) {
				{
				setState(1158);
				match(OVERRIDE);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Describe_statementContext extends ParserRuleContext {
		public TerminalNode DESCRIBE() { return getToken(KVQLParser.DESCRIBE, 0); }
		public TerminalNode DESC() { return getToken(KVQLParser.DESC, 0); }
		public TerminalNode TABLE() { return getToken(KVQLParser.TABLE, 0); }
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public TerminalNode INDEX() { return getToken(KVQLParser.INDEX, 0); }
		public Index_nameContext index_name() {
			return getRuleContext(Index_nameContext.class,0);
		}
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public TerminalNode AS() { return getToken(KVQLParser.AS, 0); }
		public TerminalNode JSON() { return getToken(KVQLParser.JSON, 0); }
		public TerminalNode LP() { return getToken(KVQLParser.LP, 0); }
		public Schema_path_listContext schema_path_list() {
			return getRuleContext(Schema_path_listContext.class,0);
		}
		public TerminalNode RP() { return getToken(KVQLParser.RP, 0); }
		public Describe_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_describe_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDescribe_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDescribe_statement(this);
		}
	}

	public final Describe_statementContext describe_statement() throws RecognitionException {
		Describe_statementContext _localctx = new Describe_statementContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_describe_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1161);
			_la = _input.LA(1);
			if ( !(_la==DESC || _la==DESCRIBE) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			setState(1164);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(1162);
				match(AS);
				setState(1163);
				match(JSON);
				}
			}

			setState(1183);
			switch (_input.LA(1)) {
			case TABLE:
				{
				setState(1166);
				match(TABLE);
				setState(1167);
				name_path();
				setState(1176);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,120,_ctx) ) {
				case 1:
					{
					{
					setState(1168);
					match(LP);
					setState(1169);
					schema_path_list();
					setState(1170);
					match(RP);
					}
					}
					break;
				case 2:
					{
					{
					setState(1172);
					match(LP);
					setState(1173);
					schema_path_list();
					 notifyErrorListeners("Missing closing ')'")
					             ; 
					}
					}
					break;
				}
				}
				break;
			case INDEX:
				{
				setState(1178);
				match(INDEX);
				setState(1179);
				index_name();
				setState(1180);
				match(ON);
				setState(1181);
				name_path();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Schema_path_listContext extends ParserRuleContext {
		public List<Schema_pathContext> schema_path() {
			return getRuleContexts(Schema_pathContext.class);
		}
		public Schema_pathContext schema_path(int i) {
			return getRuleContext(Schema_pathContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Schema_path_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schema_path_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSchema_path_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSchema_path_list(this);
		}
	}

	public final Schema_path_listContext schema_path_list() throws RecognitionException {
		Schema_path_listContext _localctx = new Schema_path_listContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_schema_path_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1185);
			schema_path();
			setState(1190);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1186);
				match(COMMA);
				setState(1187);
				schema_path();
				}
				}
				setState(1192);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Show_statementContext extends ParserRuleContext {
		public TerminalNode SHOW() { return getToken(KVQLParser.SHOW, 0); }
		public TerminalNode TABLES() { return getToken(KVQLParser.TABLES, 0); }
		public TerminalNode USERS() { return getToken(KVQLParser.USERS, 0); }
		public TerminalNode ROLES() { return getToken(KVQLParser.ROLES, 0); }
		public TerminalNode USER() { return getToken(KVQLParser.USER, 0); }
		public Identifier_or_stringContext identifier_or_string() {
			return getRuleContext(Identifier_or_stringContext.class,0);
		}
		public TerminalNode ROLE() { return getToken(KVQLParser.ROLE, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode INDEXES() { return getToken(KVQLParser.INDEXES, 0); }
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public TerminalNode TABLE() { return getToken(KVQLParser.TABLE, 0); }
		public TerminalNode AS() { return getToken(KVQLParser.AS, 0); }
		public TerminalNode JSON() { return getToken(KVQLParser.JSON, 0); }
		public Show_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_show_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterShow_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitShow_statement(this);
		}
	}

	public final Show_statementContext show_statement() throws RecognitionException {
		Show_statementContext _localctx = new Show_statementContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_show_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1193);
			match(SHOW);
			setState(1196);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(1194);
				match(AS);
				setState(1195);
				match(JSON);
				}
			}

			setState(1210);
			switch (_input.LA(1)) {
			case TABLES:
				{
				setState(1198);
				match(TABLES);
				}
				break;
			case USERS:
				{
				setState(1199);
				match(USERS);
				}
				break;
			case ROLES:
				{
				setState(1200);
				match(ROLES);
				}
				break;
			case USER:
				{
				setState(1201);
				match(USER);
				setState(1202);
				identifier_or_string();
				}
				break;
			case ROLE:
				{
				setState(1203);
				match(ROLE);
				setState(1204);
				id();
				}
				break;
			case INDEXES:
				{
				setState(1205);
				match(INDEXES);
				setState(1206);
				match(ON);
				setState(1207);
				name_path();
				}
				break;
			case TABLE:
				{
				setState(1208);
				match(TABLE);
				setState(1209);
				name_path();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Create_user_statementContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(KVQLParser.CREATE, 0); }
		public TerminalNode USER() { return getToken(KVQLParser.USER, 0); }
		public Create_user_identified_clauseContext create_user_identified_clause() {
			return getRuleContext(Create_user_identified_clauseContext.class,0);
		}
		public Account_lockContext account_lock() {
			return getRuleContext(Account_lockContext.class,0);
		}
		public TerminalNode ADMIN() { return getToken(KVQLParser.ADMIN, 0); }
		public Create_user_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_user_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCreate_user_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCreate_user_statement(this);
		}
	}

	public final Create_user_statementContext create_user_statement() throws RecognitionException {
		Create_user_statementContext _localctx = new Create_user_statementContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_create_user_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1212);
			match(CREATE);
			setState(1213);
			match(USER);
			setState(1214);
			create_user_identified_clause();
			setState(1216);
			_la = _input.LA(1);
			if (_la==ACCOUNT) {
				{
				setState(1215);
				account_lock();
				}
			}

			setState(1219);
			_la = _input.LA(1);
			if (_la==ADMIN) {
				{
				setState(1218);
				match(ADMIN);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Create_role_statementContext extends ParserRuleContext {
		public TerminalNode CREATE() { return getToken(KVQLParser.CREATE, 0); }
		public TerminalNode ROLE() { return getToken(KVQLParser.ROLE, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Create_role_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_role_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCreate_role_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCreate_role_statement(this);
		}
	}

	public final Create_role_statementContext create_role_statement() throws RecognitionException {
		Create_role_statementContext _localctx = new Create_role_statementContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_create_role_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1221);
			match(CREATE);
			setState(1222);
			match(ROLE);
			setState(1223);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Alter_user_statementContext extends ParserRuleContext {
		public TerminalNode ALTER() { return getToken(KVQLParser.ALTER, 0); }
		public TerminalNode USER() { return getToken(KVQLParser.USER, 0); }
		public Identifier_or_stringContext identifier_or_string() {
			return getRuleContext(Identifier_or_stringContext.class,0);
		}
		public Reset_password_clauseContext reset_password_clause() {
			return getRuleContext(Reset_password_clauseContext.class,0);
		}
		public TerminalNode CLEAR_RETAINED_PASSWORD() { return getToken(KVQLParser.CLEAR_RETAINED_PASSWORD, 0); }
		public TerminalNode PASSWORD_EXPIRE() { return getToken(KVQLParser.PASSWORD_EXPIRE, 0); }
		public Password_lifetimeContext password_lifetime() {
			return getRuleContext(Password_lifetimeContext.class,0);
		}
		public Account_lockContext account_lock() {
			return getRuleContext(Account_lockContext.class,0);
		}
		public Alter_user_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alter_user_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAlter_user_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAlter_user_statement(this);
		}
	}

	public final Alter_user_statementContext alter_user_statement() throws RecognitionException {
		Alter_user_statementContext _localctx = new Alter_user_statementContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_alter_user_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1225);
			match(ALTER);
			setState(1226);
			match(USER);
			setState(1227);
			identifier_or_string();
			setState(1229);
			_la = _input.LA(1);
			if (_la==IDENTIFIED) {
				{
				setState(1228);
				reset_password_clause();
				}
			}

			setState(1232);
			_la = _input.LA(1);
			if (_la==CLEAR_RETAINED_PASSWORD) {
				{
				setState(1231);
				match(CLEAR_RETAINED_PASSWORD);
				}
			}

			setState(1235);
			_la = _input.LA(1);
			if (_la==PASSWORD_EXPIRE) {
				{
				setState(1234);
				match(PASSWORD_EXPIRE);
				}
			}

			setState(1238);
			_la = _input.LA(1);
			if (_la==PASSWORD) {
				{
				setState(1237);
				password_lifetime();
				}
			}

			setState(1241);
			_la = _input.LA(1);
			if (_la==ACCOUNT) {
				{
				setState(1240);
				account_lock();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Drop_user_statementContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(KVQLParser.DROP, 0); }
		public TerminalNode USER() { return getToken(KVQLParser.USER, 0); }
		public Identifier_or_stringContext identifier_or_string() {
			return getRuleContext(Identifier_or_stringContext.class,0);
		}
		public TerminalNode CASCADE() { return getToken(KVQLParser.CASCADE, 0); }
		public Drop_user_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_drop_user_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDrop_user_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDrop_user_statement(this);
		}
	}

	public final Drop_user_statementContext drop_user_statement() throws RecognitionException {
		Drop_user_statementContext _localctx = new Drop_user_statementContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_drop_user_statement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1243);
			match(DROP);
			setState(1244);
			match(USER);
			setState(1245);
			identifier_or_string();
			setState(1247);
			_la = _input.LA(1);
			if (_la==CASCADE) {
				{
				setState(1246);
				match(CASCADE);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Drop_role_statementContext extends ParserRuleContext {
		public TerminalNode DROP() { return getToken(KVQLParser.DROP, 0); }
		public TerminalNode ROLE() { return getToken(KVQLParser.ROLE, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Drop_role_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_drop_role_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDrop_role_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDrop_role_statement(this);
		}
	}

	public final Drop_role_statementContext drop_role_statement() throws RecognitionException {
		Drop_role_statementContext _localctx = new Drop_role_statementContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_drop_role_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1249);
			match(DROP);
			setState(1250);
			match(ROLE);
			setState(1251);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Grant_statementContext extends ParserRuleContext {
		public TerminalNode GRANT() { return getToken(KVQLParser.GRANT, 0); }
		public Grant_rolesContext grant_roles() {
			return getRuleContext(Grant_rolesContext.class,0);
		}
		public Grant_system_privilegesContext grant_system_privileges() {
			return getRuleContext(Grant_system_privilegesContext.class,0);
		}
		public Grant_object_privilegesContext grant_object_privileges() {
			return getRuleContext(Grant_object_privilegesContext.class,0);
		}
		public Grant_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grant_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterGrant_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitGrant_statement(this);
		}
	}

	public final Grant_statementContext grant_statement() throws RecognitionException {
		Grant_statementContext _localctx = new Grant_statementContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_grant_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1253);
			match(GRANT);
			setState(1257);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,133,_ctx) ) {
			case 1:
				{
				setState(1254);
				grant_roles();
				}
				break;
			case 2:
				{
				setState(1255);
				grant_system_privileges();
				}
				break;
			case 3:
				{
				setState(1256);
				grant_object_privileges();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Revoke_statementContext extends ParserRuleContext {
		public TerminalNode REVOKE() { return getToken(KVQLParser.REVOKE, 0); }
		public Revoke_rolesContext revoke_roles() {
			return getRuleContext(Revoke_rolesContext.class,0);
		}
		public Revoke_system_privilegesContext revoke_system_privileges() {
			return getRuleContext(Revoke_system_privilegesContext.class,0);
		}
		public Revoke_object_privilegesContext revoke_object_privileges() {
			return getRuleContext(Revoke_object_privilegesContext.class,0);
		}
		public Revoke_statementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_revoke_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterRevoke_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitRevoke_statement(this);
		}
	}

	public final Revoke_statementContext revoke_statement() throws RecognitionException {
		Revoke_statementContext _localctx = new Revoke_statementContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_revoke_statement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1259);
			match(REVOKE);
			setState(1263);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,134,_ctx) ) {
			case 1:
				{
				setState(1260);
				revoke_roles();
				}
				break;
			case 2:
				{
				setState(1261);
				revoke_system_privileges();
				}
				break;
			case 3:
				{
				setState(1262);
				revoke_object_privileges();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Identifier_or_stringContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public Identifier_or_stringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier_or_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterIdentifier_or_string(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitIdentifier_or_string(this);
		}
	}

	public final Identifier_or_stringContext identifier_or_string() throws RecognitionException {
		Identifier_or_stringContext _localctx = new Identifier_or_stringContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_identifier_or_string);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1267);
			switch (_input.LA(1)) {
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case ID:
			case BAD_ID:
				{
				setState(1265);
				id();
				}
				break;
			case DSTRING:
			case STRING:
				{
				setState(1266);
				string();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Identified_clauseContext extends ParserRuleContext {
		public TerminalNode IDENTIFIED() { return getToken(KVQLParser.IDENTIFIED, 0); }
		public By_passwordContext by_password() {
			return getRuleContext(By_passwordContext.class,0);
		}
		public Identified_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identified_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterIdentified_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitIdentified_clause(this);
		}
	}

	public final Identified_clauseContext identified_clause() throws RecognitionException {
		Identified_clauseContext _localctx = new Identified_clauseContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_identified_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1269);
			match(IDENTIFIED);
			setState(1270);
			by_password();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Create_user_identified_clauseContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Identified_clauseContext identified_clause() {
			return getRuleContext(Identified_clauseContext.class,0);
		}
		public TerminalNode PASSWORD_EXPIRE() { return getToken(KVQLParser.PASSWORD_EXPIRE, 0); }
		public Password_lifetimeContext password_lifetime() {
			return getRuleContext(Password_lifetimeContext.class,0);
		}
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public TerminalNode IDENTIFIED_EXTERNALLY() { return getToken(KVQLParser.IDENTIFIED_EXTERNALLY, 0); }
		public Create_user_identified_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_create_user_identified_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterCreate_user_identified_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitCreate_user_identified_clause(this);
		}
	}

	public final Create_user_identified_clauseContext create_user_identified_clause() throws RecognitionException {
		Create_user_identified_clauseContext _localctx = new Create_user_identified_clauseContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_create_user_identified_clause);
		int _la;
		try {
			setState(1283);
			switch (_input.LA(1)) {
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case ID:
			case BAD_ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(1272);
				id();
				setState(1273);
				identified_clause();
				setState(1275);
				_la = _input.LA(1);
				if (_la==PASSWORD_EXPIRE) {
					{
					setState(1274);
					match(PASSWORD_EXPIRE);
					}
				}

				setState(1278);
				_la = _input.LA(1);
				if (_la==PASSWORD) {
					{
					setState(1277);
					password_lifetime();
					}
				}

				}
				break;
			case DSTRING:
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(1280);
				string();
				setState(1281);
				match(IDENTIFIED_EXTERNALLY);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class By_passwordContext extends ParserRuleContext {
		public TerminalNode BY() { return getToken(KVQLParser.BY, 0); }
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public By_passwordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_by_password; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterBy_password(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitBy_password(this);
		}
	}

	public final By_passwordContext by_password() throws RecognitionException {
		By_passwordContext _localctx = new By_passwordContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_by_password);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1285);
			match(BY);
			setState(1286);
			string();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Password_lifetimeContext extends ParserRuleContext {
		public TerminalNode PASSWORD() { return getToken(KVQLParser.PASSWORD, 0); }
		public TerminalNode LIFETIME() { return getToken(KVQLParser.LIFETIME, 0); }
		public DurationContext duration() {
			return getRuleContext(DurationContext.class,0);
		}
		public Password_lifetimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_password_lifetime; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterPassword_lifetime(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitPassword_lifetime(this);
		}
	}

	public final Password_lifetimeContext password_lifetime() throws RecognitionException {
		Password_lifetimeContext _localctx = new Password_lifetimeContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_password_lifetime);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1288);
			match(PASSWORD);
			setState(1289);
			match(LIFETIME);
			setState(1290);
			duration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Reset_password_clauseContext extends ParserRuleContext {
		public Identified_clauseContext identified_clause() {
			return getRuleContext(Identified_clauseContext.class,0);
		}
		public TerminalNode RETAIN_CURRENT_PASSWORD() { return getToken(KVQLParser.RETAIN_CURRENT_PASSWORD, 0); }
		public Reset_password_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reset_password_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterReset_password_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitReset_password_clause(this);
		}
	}

	public final Reset_password_clauseContext reset_password_clause() throws RecognitionException {
		Reset_password_clauseContext _localctx = new Reset_password_clauseContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_reset_password_clause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1292);
			identified_clause();
			setState(1294);
			_la = _input.LA(1);
			if (_la==RETAIN_CURRENT_PASSWORD) {
				{
				setState(1293);
				match(RETAIN_CURRENT_PASSWORD);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Account_lockContext extends ParserRuleContext {
		public TerminalNode ACCOUNT() { return getToken(KVQLParser.ACCOUNT, 0); }
		public TerminalNode LOCK() { return getToken(KVQLParser.LOCK, 0); }
		public TerminalNode UNLOCK() { return getToken(KVQLParser.UNLOCK, 0); }
		public Account_lockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_account_lock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterAccount_lock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitAccount_lock(this);
		}
	}

	public final Account_lockContext account_lock() throws RecognitionException {
		Account_lockContext _localctx = new Account_lockContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_account_lock);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1296);
			match(ACCOUNT);
			setState(1297);
			_la = _input.LA(1);
			if ( !(_la==LOCK || _la==UNLOCK) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Grant_rolesContext extends ParserRuleContext {
		public Id_listContext id_list() {
			return getRuleContext(Id_listContext.class,0);
		}
		public TerminalNode TO() { return getToken(KVQLParser.TO, 0); }
		public PrincipalContext principal() {
			return getRuleContext(PrincipalContext.class,0);
		}
		public Grant_rolesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grant_roles; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterGrant_roles(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitGrant_roles(this);
		}
	}

	public final Grant_rolesContext grant_roles() throws RecognitionException {
		Grant_rolesContext _localctx = new Grant_rolesContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_grant_roles);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1299);
			id_list();
			setState(1300);
			match(TO);
			setState(1301);
			principal();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Grant_system_privilegesContext extends ParserRuleContext {
		public Sys_priv_listContext sys_priv_list() {
			return getRuleContext(Sys_priv_listContext.class,0);
		}
		public TerminalNode TO() { return getToken(KVQLParser.TO, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Grant_system_privilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grant_system_privileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterGrant_system_privileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitGrant_system_privileges(this);
		}
	}

	public final Grant_system_privilegesContext grant_system_privileges() throws RecognitionException {
		Grant_system_privilegesContext _localctx = new Grant_system_privilegesContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_grant_system_privileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1303);
			sys_priv_list();
			setState(1304);
			match(TO);
			setState(1305);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Grant_object_privilegesContext extends ParserRuleContext {
		public Obj_priv_listContext obj_priv_list() {
			return getRuleContext(Obj_priv_listContext.class,0);
		}
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public ObjectContext object() {
			return getRuleContext(ObjectContext.class,0);
		}
		public TerminalNode TO() { return getToken(KVQLParser.TO, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Grant_object_privilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_grant_object_privileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterGrant_object_privileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitGrant_object_privileges(this);
		}
	}

	public final Grant_object_privilegesContext grant_object_privileges() throws RecognitionException {
		Grant_object_privilegesContext _localctx = new Grant_object_privilegesContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_grant_object_privileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1307);
			obj_priv_list();
			setState(1308);
			match(ON);
			setState(1309);
			object();
			setState(1310);
			match(TO);
			setState(1311);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Revoke_rolesContext extends ParserRuleContext {
		public Id_listContext id_list() {
			return getRuleContext(Id_listContext.class,0);
		}
		public TerminalNode FROM() { return getToken(KVQLParser.FROM, 0); }
		public PrincipalContext principal() {
			return getRuleContext(PrincipalContext.class,0);
		}
		public Revoke_rolesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_revoke_roles; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterRevoke_roles(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitRevoke_roles(this);
		}
	}

	public final Revoke_rolesContext revoke_roles() throws RecognitionException {
		Revoke_rolesContext _localctx = new Revoke_rolesContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_revoke_roles);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1313);
			id_list();
			setState(1314);
			match(FROM);
			setState(1315);
			principal();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Revoke_system_privilegesContext extends ParserRuleContext {
		public Sys_priv_listContext sys_priv_list() {
			return getRuleContext(Sys_priv_listContext.class,0);
		}
		public TerminalNode FROM() { return getToken(KVQLParser.FROM, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Revoke_system_privilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_revoke_system_privileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterRevoke_system_privileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitRevoke_system_privileges(this);
		}
	}

	public final Revoke_system_privilegesContext revoke_system_privileges() throws RecognitionException {
		Revoke_system_privilegesContext _localctx = new Revoke_system_privilegesContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_revoke_system_privileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1317);
			sys_priv_list();
			setState(1318);
			match(FROM);
			setState(1319);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Revoke_object_privilegesContext extends ParserRuleContext {
		public Obj_priv_listContext obj_priv_list() {
			return getRuleContext(Obj_priv_listContext.class,0);
		}
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public ObjectContext object() {
			return getRuleContext(ObjectContext.class,0);
		}
		public TerminalNode FROM() { return getToken(KVQLParser.FROM, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Revoke_object_privilegesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_revoke_object_privileges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterRevoke_object_privileges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitRevoke_object_privileges(this);
		}
	}

	public final Revoke_object_privilegesContext revoke_object_privileges() throws RecognitionException {
		Revoke_object_privilegesContext _localctx = new Revoke_object_privilegesContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_revoke_object_privileges);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1321);
			obj_priv_list();
			setState(1322);
			match(ON);
			setState(1323);
			object();
			setState(1324);
			match(FROM);
			setState(1325);
			id();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrincipalContext extends ParserRuleContext {
		public TerminalNode USER() { return getToken(KVQLParser.USER, 0); }
		public Identifier_or_stringContext identifier_or_string() {
			return getRuleContext(Identifier_or_stringContext.class,0);
		}
		public TerminalNode ROLE() { return getToken(KVQLParser.ROLE, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public PrincipalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_principal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterPrincipal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitPrincipal(this);
		}
	}

	public final PrincipalContext principal() throws RecognitionException {
		PrincipalContext _localctx = new PrincipalContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_principal);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1331);
			switch (_input.LA(1)) {
			case USER:
				{
				setState(1327);
				match(USER);
				setState(1328);
				identifier_or_string();
				}
				break;
			case ROLE:
				{
				setState(1329);
				match(ROLE);
				setState(1330);
				id();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Sys_priv_listContext extends ParserRuleContext {
		public List<Priv_itemContext> priv_item() {
			return getRuleContexts(Priv_itemContext.class);
		}
		public Priv_itemContext priv_item(int i) {
			return getRuleContext(Priv_itemContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Sys_priv_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sys_priv_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterSys_priv_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitSys_priv_list(this);
		}
	}

	public final Sys_priv_listContext sys_priv_list() throws RecognitionException {
		Sys_priv_listContext _localctx = new Sys_priv_listContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_sys_priv_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1333);
			priv_item();
			setState(1338);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1334);
				match(COMMA);
				setState(1335);
				priv_item();
				}
				}
				setState(1340);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Priv_itemContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode ALL_PRIVILEGES() { return getToken(KVQLParser.ALL_PRIVILEGES, 0); }
		public Priv_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_priv_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterPriv_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitPriv_item(this);
		}
	}

	public final Priv_itemContext priv_item() throws RecognitionException {
		Priv_itemContext _localctx = new Priv_itemContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_priv_item);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1343);
			switch (_input.LA(1)) {
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case ID:
			case BAD_ID:
				{
				setState(1341);
				id();
				}
				break;
			case ALL_PRIVILEGES:
				{
				setState(1342);
				match(ALL_PRIVILEGES);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Obj_priv_listContext extends ParserRuleContext {
		public List<Priv_itemContext> priv_item() {
			return getRuleContexts(Priv_itemContext.class);
		}
		public Priv_itemContext priv_item(int i) {
			return getRuleContext(Priv_itemContext.class,i);
		}
		public List<TerminalNode> ALL() { return getTokens(KVQLParser.ALL); }
		public TerminalNode ALL(int i) {
			return getToken(KVQLParser.ALL, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Obj_priv_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_obj_priv_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterObj_priv_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitObj_priv_list(this);
		}
	}

	public final Obj_priv_listContext obj_priv_list() throws RecognitionException {
		Obj_priv_listContext _localctx = new Obj_priv_listContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_obj_priv_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1347);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
			case 1:
				{
				setState(1345);
				priv_item();
				}
				break;
			case 2:
				{
				setState(1346);
				match(ALL);
				}
				break;
			}
			setState(1356);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1349);
				match(COMMA);
				setState(1352);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
				case 1:
					{
					setState(1350);
					priv_item();
					}
					break;
				case 2:
					{
					setState(1351);
					match(ALL);
					}
					break;
				}
				}
				}
				setState(1358);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectContext extends ParserRuleContext {
		public Name_pathContext name_path() {
			return getRuleContext(Name_pathContext.class,0);
		}
		public ObjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_object; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterObject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitObject(this);
		}
	}

	public final ObjectContext object() throws RecognitionException {
		ObjectContext _localctx = new ObjectContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_object);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1359);
			name_path();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Json_textContext extends ParserRuleContext {
		public JsobjectContext jsobject() {
			return getRuleContext(JsobjectContext.class,0);
		}
		public JsarrayContext jsarray() {
			return getRuleContext(JsarrayContext.class,0);
		}
		public Json_textContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_json_text; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJson_text(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJson_text(this);
		}
	}

	public final Json_textContext json_text() throws RecognitionException {
		Json_textContext _localctx = new Json_textContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_json_text);
		try {
			setState(1363);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1361);
				jsobject();
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 2);
				{
				setState(1362);
				jsarray();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JsobjectContext extends ParserRuleContext {
		public JsobjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsobject; }
	 
		public JsobjectContext() { }
		public void copyFrom(JsobjectContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class JsonObjectContext extends JsobjectContext {
		public TerminalNode LBRACE() { return getToken(KVQLParser.LBRACE, 0); }
		public List<JspairContext> jspair() {
			return getRuleContexts(JspairContext.class);
		}
		public JspairContext jspair(int i) {
			return getRuleContext(JspairContext.class,i);
		}
		public TerminalNode RBRACE() { return getToken(KVQLParser.RBRACE, 0); }
		public JsonObjectContext(JsobjectContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJsonObject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJsonObject(this);
		}
	}
	public static class EmptyJsonObjectContext extends JsobjectContext {
		public TerminalNode LBRACE() { return getToken(KVQLParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(KVQLParser.RBRACE, 0); }
		public EmptyJsonObjectContext(JsobjectContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterEmptyJsonObject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitEmptyJsonObject(this);
		}
	}

	public final JsobjectContext jsobject() throws RecognitionException {
		JsobjectContext _localctx = new JsobjectContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_jsobject);
		int _la;
		try {
			setState(1378);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,148,_ctx) ) {
			case 1:
				_localctx = new JsonObjectContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1365);
				match(LBRACE);
				setState(1366);
				jspair();
				setState(1371);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1367);
					match(COMMA);
					setState(1368);
					jspair();
					}
					}
					setState(1373);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1374);
				match(RBRACE);
				}
				break;
			case 2:
				_localctx = new EmptyJsonObjectContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1376);
				match(LBRACE);
				setState(1377);
				match(RBRACE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JsarrayContext extends ParserRuleContext {
		public JsarrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsarray; }
	 
		public JsarrayContext() { }
		public void copyFrom(JsarrayContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class EmptyJsonArrayContext extends JsarrayContext {
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public EmptyJsonArrayContext(JsarrayContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterEmptyJsonArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitEmptyJsonArray(this);
		}
	}
	public static class ArrayOfJsonValuesContext extends JsarrayContext {
		public TerminalNode LBRACK() { return getToken(KVQLParser.LBRACK, 0); }
		public List<JsvalueContext> jsvalue() {
			return getRuleContexts(JsvalueContext.class);
		}
		public JsvalueContext jsvalue(int i) {
			return getRuleContext(JsvalueContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(KVQLParser.RBRACK, 0); }
		public ArrayOfJsonValuesContext(JsarrayContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterArrayOfJsonValues(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitArrayOfJsonValues(this);
		}
	}

	public final JsarrayContext jsarray() throws RecognitionException {
		JsarrayContext _localctx = new JsarrayContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_jsarray);
		int _la;
		try {
			setState(1393);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,150,_ctx) ) {
			case 1:
				_localctx = new ArrayOfJsonValuesContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1380);
				match(LBRACK);
				setState(1381);
				jsvalue();
				setState(1386);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(1382);
					match(COMMA);
					setState(1383);
					jsvalue();
					}
					}
					setState(1388);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1389);
				match(RBRACK);
				}
				break;
			case 2:
				_localctx = new EmptyJsonArrayContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1391);
				match(LBRACK);
				setState(1392);
				match(RBRACK);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JspairContext extends ParserRuleContext {
		public JspairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jspair; }
	 
		public JspairContext() { }
		public void copyFrom(JspairContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class JsonPairContext extends JspairContext {
		public TerminalNode DSTRING() { return getToken(KVQLParser.DSTRING, 0); }
		public JsvalueContext jsvalue() {
			return getRuleContext(JsvalueContext.class,0);
		}
		public JsonPairContext(JspairContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJsonPair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJsonPair(this);
		}
	}

	public final JspairContext jspair() throws RecognitionException {
		JspairContext _localctx = new JspairContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_jspair);
		try {
			_localctx = new JsonPairContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(1395);
			match(DSTRING);
			setState(1396);
			match(COLON);
			setState(1397);
			jsvalue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class JsvalueContext extends ParserRuleContext {
		public JsvalueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsvalue; }
	 
		public JsvalueContext() { }
		public void copyFrom(JsvalueContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class JsonAtomContext extends JsvalueContext {
		public TerminalNode DSTRING() { return getToken(KVQLParser.DSTRING, 0); }
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public TerminalNode TRUE() { return getToken(KVQLParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(KVQLParser.FALSE, 0); }
		public TerminalNode NULL() { return getToken(KVQLParser.NULL, 0); }
		public JsonAtomContext(JsvalueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJsonAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJsonAtom(this);
		}
	}
	public static class JsonArrayValueContext extends JsvalueContext {
		public JsarrayContext jsarray() {
			return getRuleContext(JsarrayContext.class,0);
		}
		public JsonArrayValueContext(JsvalueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJsonArrayValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJsonArrayValue(this);
		}
	}
	public static class JsonObjectValueContext extends JsvalueContext {
		public JsobjectContext jsobject() {
			return getRuleContext(JsobjectContext.class,0);
		}
		public JsonObjectValueContext(JsvalueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterJsonObjectValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitJsonObjectValue(this);
		}
	}

	public final JsvalueContext jsvalue() throws RecognitionException {
		JsvalueContext _localctx = new JsvalueContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_jsvalue);
		try {
			setState(1406);
			switch (_input.LA(1)) {
			case LBRACE:
				_localctx = new JsonObjectValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(1399);
				jsobject();
				}
				break;
			case LBRACK:
				_localctx = new JsonArrayValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(1400);
				jsarray();
				}
				break;
			case DSTRING:
				_localctx = new JsonAtomContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(1401);
				match(DSTRING);
				}
				break;
			case MINUS:
			case INT:
			case FLOAT:
				_localctx = new JsonAtomContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(1402);
				number();
				}
				break;
			case TRUE:
				_localctx = new JsonAtomContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(1403);
				match(TRUE);
				}
				break;
			case FALSE:
				_localctx = new JsonAtomContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(1404);
				match(FALSE);
				}
				break;
			case NULL:
				_localctx = new JsonAtomContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(1405);
				match(NULL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CommentContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(KVQLParser.COMMENT, 0); }
		public StringContext string() {
			return getRuleContext(StringContext.class,0);
		}
		public CommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitComment(this);
		}
	}

	public final CommentContext comment() throws RecognitionException {
		CommentContext _localctx = new CommentContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1408);
			match(COMMENT);
			setState(1409);
			string();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DurationContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public TerminalNode TIME_UNIT() { return getToken(KVQLParser.TIME_UNIT, 0); }
		public DurationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_duration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterDuration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitDuration(this);
		}
	}

	public final DurationContext duration() throws RecognitionException {
		DurationContext _localctx = new DurationContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_duration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1411);
			match(INT);
			setState(1412);
			match(TIME_UNIT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumberContext extends ParserRuleContext {
		public TerminalNode FLOAT() { return getToken(KVQLParser.FLOAT, 0); }
		public TerminalNode INT() { return getToken(KVQLParser.INT, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitNumber(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1415);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(1414);
				match(MINUS);
				}
			}

			setState(1417);
			_la = _input.LA(1);
			if ( !(_la==INT || _la==FLOAT) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StringContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(KVQLParser.STRING, 0); }
		public TerminalNode DSTRING() { return getToken(KVQLParser.DSTRING, 0); }
		public StringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_string; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitString(this);
		}
	}

	public final StringContext string() throws RecognitionException {
		StringContext _localctx = new StringContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_string);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1419);
			_la = _input.LA(1);
			if ( !(_la==DSTRING || _la==STRING) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Id_listContext extends ParserRuleContext {
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(KVQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(KVQLParser.COMMA, i);
		}
		public Id_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterId_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitId_list(this);
		}
	}

	public final Id_listContext id_list() throws RecognitionException {
		Id_listContext _localctx = new Id_listContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_id_list);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1421);
			id();
			setState(1426);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,153,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1422);
					match(COMMA);
					setState(1423);
					id();
					}
					} 
				}
				setState(1428);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,153,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdContext extends ParserRuleContext {
		public TerminalNode ACCOUNT() { return getToken(KVQLParser.ACCOUNT, 0); }
		public TerminalNode ADD() { return getToken(KVQLParser.ADD, 0); }
		public TerminalNode ADMIN() { return getToken(KVQLParser.ADMIN, 0); }
		public TerminalNode ALL() { return getToken(KVQLParser.ALL, 0); }
		public TerminalNode ALTER() { return getToken(KVQLParser.ALTER, 0); }
		public TerminalNode AND() { return getToken(KVQLParser.AND, 0); }
		public TerminalNode ANY_T() { return getToken(KVQLParser.ANY_T, 0); }
		public TerminalNode ANYATOMIC_T() { return getToken(KVQLParser.ANYATOMIC_T, 0); }
		public TerminalNode ANYJSONATOMIC_T() { return getToken(KVQLParser.ANYJSONATOMIC_T, 0); }
		public TerminalNode ANYRECORD_T() { return getToken(KVQLParser.ANYRECORD_T, 0); }
		public TerminalNode AS() { return getToken(KVQLParser.AS, 0); }
		public TerminalNode ASC() { return getToken(KVQLParser.ASC, 0); }
		public TerminalNode BY() { return getToken(KVQLParser.BY, 0); }
		public TerminalNode CASE() { return getToken(KVQLParser.CASE, 0); }
		public TerminalNode CAST() { return getToken(KVQLParser.CAST, 0); }
		public TerminalNode COMMENT() { return getToken(KVQLParser.COMMENT, 0); }
		public TerminalNode CREATE() { return getToken(KVQLParser.CREATE, 0); }
		public TerminalNode DECLARE() { return getToken(KVQLParser.DECLARE, 0); }
		public TerminalNode DEFAULT() { return getToken(KVQLParser.DEFAULT, 0); }
		public TerminalNode DESC() { return getToken(KVQLParser.DESC, 0); }
		public TerminalNode DESCRIBE() { return getToken(KVQLParser.DESCRIBE, 0); }
		public TerminalNode DROP() { return getToken(KVQLParser.DROP, 0); }
		public TerminalNode ELEMENTOF() { return getToken(KVQLParser.ELEMENTOF, 0); }
		public TerminalNode ELSE() { return getToken(KVQLParser.ELSE, 0); }
		public TerminalNode END() { return getToken(KVQLParser.END, 0); }
		public TerminalNode ES_SHARDS() { return getToken(KVQLParser.ES_SHARDS, 0); }
		public TerminalNode ES_REPLICAS() { return getToken(KVQLParser.ES_REPLICAS, 0); }
		public TerminalNode EXISTS() { return getToken(KVQLParser.EXISTS, 0); }
		public TerminalNode FIRST() { return getToken(KVQLParser.FIRST, 0); }
		public TerminalNode FROM() { return getToken(KVQLParser.FROM, 0); }
		public TerminalNode FULLTEXT() { return getToken(KVQLParser.FULLTEXT, 0); }
		public TerminalNode GRANT() { return getToken(KVQLParser.GRANT, 0); }
		public TerminalNode IDENTIFIED() { return getToken(KVQLParser.IDENTIFIED, 0); }
		public TerminalNode IF() { return getToken(KVQLParser.IF, 0); }
		public TerminalNode INDEX() { return getToken(KVQLParser.INDEX, 0); }
		public TerminalNode INDEXES() { return getToken(KVQLParser.INDEXES, 0); }
		public TerminalNode IS() { return getToken(KVQLParser.IS, 0); }
		public TerminalNode JSON() { return getToken(KVQLParser.JSON, 0); }
		public TerminalNode KEY() { return getToken(KVQLParser.KEY, 0); }
		public TerminalNode KEYOF() { return getToken(KVQLParser.KEYOF, 0); }
		public TerminalNode KEYS() { return getToken(KVQLParser.KEYS, 0); }
		public TerminalNode LIFETIME() { return getToken(KVQLParser.LIFETIME, 0); }
		public TerminalNode LAST() { return getToken(KVQLParser.LAST, 0); }
		public TerminalNode LIMIT() { return getToken(KVQLParser.LIMIT, 0); }
		public TerminalNode LOCK() { return getToken(KVQLParser.LOCK, 0); }
		public TerminalNode MODIFY() { return getToken(KVQLParser.MODIFY, 0); }
		public TerminalNode NOT() { return getToken(KVQLParser.NOT, 0); }
		public TerminalNode NULLS() { return getToken(KVQLParser.NULLS, 0); }
		public TerminalNode OF() { return getToken(KVQLParser.OF, 0); }
		public TerminalNode OFFSET() { return getToken(KVQLParser.OFFSET, 0); }
		public TerminalNode ON() { return getToken(KVQLParser.ON, 0); }
		public TerminalNode OR() { return getToken(KVQLParser.OR, 0); }
		public TerminalNode ORDER() { return getToken(KVQLParser.ORDER, 0); }
		public TerminalNode OVERRIDE() { return getToken(KVQLParser.OVERRIDE, 0); }
		public TerminalNode PASSWORD() { return getToken(KVQLParser.PASSWORD, 0); }
		public TerminalNode PRIMARY() { return getToken(KVQLParser.PRIMARY, 0); }
		public TerminalNode ROLE() { return getToken(KVQLParser.ROLE, 0); }
		public TerminalNode ROLES() { return getToken(KVQLParser.ROLES, 0); }
		public TerminalNode REVOKE() { return getToken(KVQLParser.REVOKE, 0); }
		public TerminalNode SELECT() { return getToken(KVQLParser.SELECT, 0); }
		public TerminalNode SHARD() { return getToken(KVQLParser.SHARD, 0); }
		public TerminalNode SHOW() { return getToken(KVQLParser.SHOW, 0); }
		public TerminalNode TABLE() { return getToken(KVQLParser.TABLE, 0); }
		public TerminalNode TABLES() { return getToken(KVQLParser.TABLES, 0); }
		public TerminalNode THEN() { return getToken(KVQLParser.THEN, 0); }
		public TerminalNode TIME_UNIT() { return getToken(KVQLParser.TIME_UNIT, 0); }
		public TerminalNode TO() { return getToken(KVQLParser.TO, 0); }
		public TerminalNode TTL() { return getToken(KVQLParser.TTL, 0); }
		public TerminalNode TYPE() { return getToken(KVQLParser.TYPE, 0); }
		public TerminalNode UNLOCK() { return getToken(KVQLParser.UNLOCK, 0); }
		public TerminalNode USER() { return getToken(KVQLParser.USER, 0); }
		public TerminalNode USERS() { return getToken(KVQLParser.USERS, 0); }
		public TerminalNode USING() { return getToken(KVQLParser.USING, 0); }
		public TerminalNode VALUES() { return getToken(KVQLParser.VALUES, 0); }
		public TerminalNode WHEN() { return getToken(KVQLParser.WHEN, 0); }
		public TerminalNode WHERE() { return getToken(KVQLParser.WHERE, 0); }
		public TerminalNode ARRAY_T() { return getToken(KVQLParser.ARRAY_T, 0); }
		public TerminalNode BINARY_T() { return getToken(KVQLParser.BINARY_T, 0); }
		public TerminalNode BOOLEAN_T() { return getToken(KVQLParser.BOOLEAN_T, 0); }
		public TerminalNode DOUBLE_T() { return getToken(KVQLParser.DOUBLE_T, 0); }
		public TerminalNode ENUM_T() { return getToken(KVQLParser.ENUM_T, 0); }
		public TerminalNode FLOAT_T() { return getToken(KVQLParser.FLOAT_T, 0); }
		public TerminalNode LONG_T() { return getToken(KVQLParser.LONG_T, 0); }
		public TerminalNode INTEGER_T() { return getToken(KVQLParser.INTEGER_T, 0); }
		public TerminalNode MAP_T() { return getToken(KVQLParser.MAP_T, 0); }
		public TerminalNode NUMBER_T() { return getToken(KVQLParser.NUMBER_T, 0); }
		public TerminalNode RECORD_T() { return getToken(KVQLParser.RECORD_T, 0); }
		public TerminalNode STRING_T() { return getToken(KVQLParser.STRING_T, 0); }
		public TerminalNode TIMESTAMP_T() { return getToken(KVQLParser.TIMESTAMP_T, 0); }
		public TerminalNode ID() { return getToken(KVQLParser.ID, 0); }
		public TerminalNode BAD_ID() { return getToken(KVQLParser.BAD_ID, 0); }
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof KVQLListener ) ((KVQLListener)listener).exitId(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_id);
		int _la;
		try {
			setState(1432);
			switch (_input.LA(1)) {
			case ACCOUNT:
			case ADD:
			case ADMIN:
			case ALL:
			case ALTER:
			case AND:
			case AS:
			case ASC:
			case BY:
			case CASE:
			case CAST:
			case COMMENT:
			case CREATE:
			case DECLARE:
			case DEFAULT:
			case DESC:
			case DESCRIBE:
			case DROP:
			case ELEMENTOF:
			case ELSE:
			case END:
			case ES_SHARDS:
			case ES_REPLICAS:
			case EXISTS:
			case FIRST:
			case FROM:
			case FULLTEXT:
			case GRANT:
			case IDENTIFIED:
			case IF:
			case INDEX:
			case INDEXES:
			case IS:
			case JSON:
			case KEY:
			case KEYOF:
			case KEYS:
			case LAST:
			case LIFETIME:
			case LIMIT:
			case LOCK:
			case MODIFY:
			case NOT:
			case NULLS:
			case OFFSET:
			case OF:
			case ON:
			case OR:
			case ORDER:
			case OVERRIDE:
			case PASSWORD:
			case PRIMARY:
			case REVOKE:
			case ROLE:
			case ROLES:
			case SELECT:
			case SHARD:
			case SHOW:
			case TABLE:
			case TABLES:
			case THEN:
			case TIME_UNIT:
			case TO:
			case TTL:
			case TYPE:
			case UNLOCK:
			case USER:
			case USERS:
			case USING:
			case VALUES:
			case WHEN:
			case WHERE:
			case ARRAY_T:
			case BINARY_T:
			case BOOLEAN_T:
			case DOUBLE_T:
			case ENUM_T:
			case FLOAT_T:
			case INTEGER_T:
			case LONG_T:
			case MAP_T:
			case NUMBER_T:
			case RECORD_T:
			case STRING_T:
			case TIMESTAMP_T:
			case ANY_T:
			case ANYATOMIC_T:
			case ANYJSONATOMIC_T:
			case ANYRECORD_T:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(1429);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ACCOUNT) | (1L << ADD) | (1L << ADMIN) | (1L << ALL) | (1L << ALTER) | (1L << AND) | (1L << AS) | (1L << ASC) | (1L << BY) | (1L << CASE) | (1L << CAST) | (1L << COMMENT) | (1L << CREATE) | (1L << DECLARE) | (1L << DEFAULT) | (1L << DESC) | (1L << DESCRIBE) | (1L << DROP) | (1L << ELEMENTOF) | (1L << ELSE) | (1L << END) | (1L << ES_SHARDS) | (1L << ES_REPLICAS) | (1L << EXISTS) | (1L << FIRST) | (1L << FROM) | (1L << FULLTEXT) | (1L << GRANT) | (1L << IDENTIFIED) | (1L << IF) | (1L << INDEX) | (1L << INDEXES) | (1L << IS) | (1L << JSON) | (1L << KEY) | (1L << KEYOF) | (1L << KEYS) | (1L << LAST) | (1L << LIFETIME) | (1L << LIMIT) | (1L << LOCK) | (1L << MODIFY) | (1L << NOT) | (1L << NULLS) | (1L << OFFSET) | (1L << OF) | (1L << ON) | (1L << OR) | (1L << ORDER) | (1L << OVERRIDE) | (1L << PASSWORD) | (1L << PRIMARY) | (1L << REVOKE) | (1L << ROLE) | (1L << ROLES))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SELECT - 64)) | (1L << (SHARD - 64)) | (1L << (SHOW - 64)) | (1L << (TABLE - 64)) | (1L << (TABLES - 64)) | (1L << (THEN - 64)) | (1L << (TIME_UNIT - 64)) | (1L << (TO - 64)) | (1L << (TTL - 64)) | (1L << (TYPE - 64)) | (1L << (UNLOCK - 64)) | (1L << (USER - 64)) | (1L << (USERS - 64)) | (1L << (USING - 64)) | (1L << (VALUES - 64)) | (1L << (WHEN - 64)) | (1L << (WHERE - 64)) | (1L << (ARRAY_T - 64)) | (1L << (BINARY_T - 64)) | (1L << (BOOLEAN_T - 64)) | (1L << (DOUBLE_T - 64)) | (1L << (ENUM_T - 64)) | (1L << (FLOAT_T - 64)) | (1L << (INTEGER_T - 64)) | (1L << (LONG_T - 64)) | (1L << (MAP_T - 64)) | (1L << (NUMBER_T - 64)) | (1L << (RECORD_T - 64)) | (1L << (STRING_T - 64)) | (1L << (TIMESTAMP_T - 64)) | (1L << (ANY_T - 64)) | (1L << (ANYATOMIC_T - 64)) | (1L << (ANYJSONATOMIC_T - 64)) | (1L << (ANYRECORD_T - 64)))) != 0) || _la==ID) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
				break;
			case BAD_ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1430);
				match(BAD_ID);

				        notifyErrorListeners("Identifiers must start with a letter: " + _input.getText(_localctx.start, _input.LT(-1)));
				     
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 19:
			return or_expr_sempred((Or_exprContext)_localctx, predIndex);
		case 20:
			return and_expr_sempred((And_exprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean or_expr_sempred(Or_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean and_expr_sempred(And_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u0092\u059d\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k\t"+
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv\4"+
		"w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t\u0080"+
		"\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084\4\u0085"+
		"\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089\t\u0089"+
		"\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d\4\u008e"+
		"\t\u008e\4\u008f\t\u008f\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3\u0132\n\3\3\4\5\4\u0135\n\4\3\4\3\4"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\7\5\u013f\n\5\f\5\16\5\u0142\13\5\3\6\3\6\3\6"+
		"\3\7\3\7\3\7\3\b\3\b\3\t\3\t\3\t\5\t\u014f\n\t\3\t\5\t\u0152\n\t\3\t\5"+
		"\t\u0155\n\t\3\t\5\t\u0158\n\t\3\n\3\n\3\n\5\n\u015d\n\n\3\n\5\n\u0160"+
		"\n\n\3\n\3\n\3\n\5\n\u0165\n\n\3\n\3\n\7\n\u0169\n\n\f\n\16\n\u016c\13"+
		"\n\3\13\5\13\u016f\n\13\3\13\3\13\3\f\3\f\3\f\3\r\3\r\5\r\u0178\n\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\7\r\u0181\n\r\f\r\16\r\u0184\13\r\5\r\u0186"+
		"\n\r\3\16\3\16\7\16\u018a\n\16\f\16\16\16\u018d\13\16\3\16\3\16\3\17\3"+
		"\17\3\17\3\17\7\17\u0195\n\17\f\17\16\17\u0198\13\17\3\17\3\17\3\17\3"+
		"\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3"+
		"\17\5\17\u01ac\n\17\3\17\5\17\u01af\n\17\3\20\3\20\5\20\u01b3\n\20\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\7\21\u01bd\n\21\f\21\16\21\u01c0\13"+
		"\21\3\22\5\22\u01c3\n\22\3\22\3\22\5\22\u01c7\n\22\3\23\3\23\3\23\3\24"+
		"\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\7\25\u01d5\n\25\f\25\16\25\u01d8"+
		"\13\25\3\26\3\26\3\26\3\26\3\26\3\26\7\26\u01e0\n\26\f\26\16\26\u01e3"+
		"\13\26\3\27\5\27\u01e6\n\27\3\27\3\27\3\30\3\30\3\30\5\30\u01ed\n\30\3"+
		"\31\3\31\3\31\3\32\3\32\3\32\5\32\u01f5\n\32\3\32\3\32\5\32\u01f9\n\32"+
		"\3\32\3\32\5\32\u01fd\n\32\3\32\3\32\3\32\5\32\u0202\n\32\3\32\7\32\u0205"+
		"\n\32\f\32\16\32\u0208\13\32\3\32\3\32\3\33\3\33\3\33\5\33\u020f\n\33"+
		"\3\33\3\33\5\33\u0213\n\33\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\5\35"+
		"\u021d\n\35\3\36\3\36\3\36\7\36\u0222\n\36\f\36\16\36\u0225\13\36\3\37"+
		"\3\37\3\37\7\37\u022a\n\37\f\37\16\37\u022d\13\37\3 \3 \3 \5 \u0232\n"+
		" \3!\3!\3!\7!\u0237\n!\f!\16!\u023a\13!\3\"\3\"\3\"\5\"\u023f\n\"\3#\3"+
		"#\3#\3#\3#\5#\u0246\n#\3$\3$\3$\5$\u024b\n$\3$\3$\3%\3%\5%\u0251\n%\3"+
		"&\3&\5&\u0255\n&\3&\3&\5&\u0259\n&\3&\3&\3\'\3\'\5\'\u025f\n\'\3\'\3\'"+
		"\3(\3(\3(\3(\3(\3(\3(\3(\3(\5(\u026c\n(\3)\3)\3)\5)\u0271\n)\3*\3*\3*"+
		"\3*\3*\3*\5*\u0279\n*\3+\3+\5+\u027d\n+\3,\3,\5,\u0281\n,\3,\3,\7,\u0285"+
		"\n,\f,\16,\u0288\13,\3,\3,\3-\3-\3-\3-\3-\3-\3-\3-\3-\7-\u0295\n-\f-\16"+
		"-\u0298\13-\3-\3-\3-\3-\5-\u029e\n-\3.\3.\3.\3.\3.\7.\u02a5\n.\f.\16."+
		"\u02a8\13.\5.\u02aa\n.\3.\3.\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\7/\u02b8\n"+
		"/\f/\16/\u02bb\13/\3/\3/\5/\u02bf\n/\3/\3/\3\60\3\60\3\60\3\60\3\60\3"+
		"\60\3\60\3\61\3\61\3\61\3\61\3\62\3\62\5\62\u02d0\n\62\3\63\3\63\3\63"+
		"\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\5\63\u02e1"+
		"\n\63\3\64\3\64\3\64\3\64\3\64\7\64\u02e8\n\64\f\64\16\64\u02eb\13\64"+
		"\3\64\3\64\3\65\3\65\3\65\5\65\u02f2\n\65\3\65\5\65\u02f5\n\65\3\66\3"+
		"\66\5\66\u02f9\n\66\3\66\3\66\5\66\u02fd\n\66\5\66\u02ff\n\66\3\67\3\67"+
		"\3\67\3\67\3\67\3\67\5\67\u0307\n\67\38\38\38\39\39\39\39\39\3:\3:\3:"+
		"\3:\3:\3;\3;\3<\3<\3=\3=\3>\3>\3?\3?\3?\3?\3?\3?\3?\3?\3?\3?\5?\u0328"+
		"\n?\3@\3@\3A\3A\3A\3A\5A\u0330\nA\3B\3B\3B\3B\5B\u0336\nB\3C\3C\3D\3D"+
		"\3E\3E\3F\3F\3G\3G\3G\7G\u0343\nG\fG\16G\u0346\13G\3H\3H\3H\3H\3H\5H\u034d"+
		"\nH\3H\3H\5H\u0351\nH\3H\3H\3H\3H\5H\u0357\nH\3I\3I\3J\3J\5J\u035d\nJ"+
		"\3J\3J\3J\5J\u0362\nJ\7J\u0364\nJ\fJ\16J\u0367\13J\3K\3K\3K\3K\3K\5K\u036e"+
		"\nK\5K\u0370\nK\3K\5K\u0373\nK\3K\3K\3L\3L\3L\3L\3L\3L\3L\3L\3L\5L\u0380"+
		"\nL\3M\3M\3M\7M\u0385\nM\fM\16M\u0388\13M\3N\3N\5N\u038c\nN\3O\3O\3O\3"+
		"O\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3Q\3R\3R\5R\u039d\nR\3S\3S\3S\3S\5S\u03a3\n"+
		"S\3S\3S\3S\3S\5S\u03a9\nS\7S\u03ab\nS\fS\16S\u03ae\13S\3S\3S\3T\3T\3T"+
		"\3T\5T\u03b6\nT\3T\5T\u03b9\nT\3U\3U\3U\3V\3V\3V\3V\5V\u03c2\nV\3V\5V"+
		"\u03c5\nV\3W\3W\3W\7W\u03ca\nW\fW\16W\u03cd\13W\3X\3X\3X\7X\u03d2\nX\f"+
		"X\16X\u03d5\13X\3Y\3Y\3Y\7Y\u03da\nY\fY\16Y\u03dd\13Y\3Y\3Y\3Y\5Y\u03e2"+
		"\nY\3Z\3Z\3Z\3Z\5Z\u03e8\nZ\3Z\3Z\3[\3[\3[\3[\3[\5[\u03f1\n[\3[\3[\3["+
		"\3[\3[\3[\3[\3[\3[\3[\3[\5[\u03fe\n[\3[\5[\u0401\n[\3\\\3\\\3]\3]\3]\7"+
		"]\u0408\n]\f]\16]\u040b\13]\3^\3^\3^\3^\5^\u0411\n^\3_\3_\3_\3_\3_\3_"+
		"\3_\3_\3_\3_\3_\3_\3_\3_\3_\3_\5_\u0423\n_\3`\3`\3`\3`\3`\3`\3`\5`\u042c"+
		"\n`\3`\3`\3`\3`\3`\3`\3`\3`\3`\5`\u0437\n`\3`\3`\5`\u043b\n`\5`\u043d"+
		"\n`\3a\3a\3a\3a\3a\5a\u0444\na\3b\3b\3b\3b\3b\3b\5b\u044c\nb\3b\3b\3b"+
		"\3b\3b\5b\u0453\nb\3b\5b\u0456\nb\3b\5b\u0459\nb\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\5c\u0463\nc\3d\3d\3d\7d\u0468\nd\fd\16d\u046b\13d\3e\3e\5e\u046f\n"+
		"e\3f\3f\7f\u0473\nf\ff\16f\u0476\13f\3g\3g\3g\3g\3g\3g\5g\u047e\ng\3h"+
		"\3h\3h\3h\5h\u0484\nh\3h\3h\3h\3h\5h\u048a\nh\3i\3i\3i\5i\u048f\ni\3i"+
		"\3i\3i\3i\3i\3i\3i\3i\3i\3i\5i\u049b\ni\3i\3i\3i\3i\3i\5i\u04a2\ni\3j"+
		"\3j\3j\7j\u04a7\nj\fj\16j\u04aa\13j\3k\3k\3k\5k\u04af\nk\3k\3k\3k\3k\3"+
		"k\3k\3k\3k\3k\3k\3k\3k\5k\u04bd\nk\3l\3l\3l\3l\5l\u04c3\nl\3l\5l\u04c6"+
		"\nl\3m\3m\3m\3m\3n\3n\3n\3n\5n\u04d0\nn\3n\5n\u04d3\nn\3n\5n\u04d6\nn"+
		"\3n\5n\u04d9\nn\3n\5n\u04dc\nn\3o\3o\3o\3o\5o\u04e2\no\3p\3p\3p\3p\3q"+
		"\3q\3q\3q\5q\u04ec\nq\3r\3r\3r\3r\5r\u04f2\nr\3s\3s\5s\u04f6\ns\3t\3t"+
		"\3t\3u\3u\3u\5u\u04fe\nu\3u\5u\u0501\nu\3u\3u\3u\5u\u0506\nu\3v\3v\3v"+
		"\3w\3w\3w\3w\3x\3x\5x\u0511\nx\3y\3y\3y\3z\3z\3z\3z\3{\3{\3{\3{\3|\3|"+
		"\3|\3|\3|\3|\3}\3}\3}\3}\3~\3~\3~\3~\3\177\3\177\3\177\3\177\3\177\3\177"+
		"\3\u0080\3\u0080\3\u0080\3\u0080\5\u0080\u0536\n\u0080\3\u0081\3\u0081"+
		"\3\u0081\7\u0081\u053b\n\u0081\f\u0081\16\u0081\u053e\13\u0081\3\u0082"+
		"\3\u0082\5\u0082\u0542\n\u0082\3\u0083\3\u0083\5\u0083\u0546\n\u0083\3"+
		"\u0083\3\u0083\3\u0083\5\u0083\u054b\n\u0083\7\u0083\u054d\n\u0083\f\u0083"+
		"\16\u0083\u0550\13\u0083\3\u0084\3\u0084\3\u0085\3\u0085\5\u0085\u0556"+
		"\n\u0085\3\u0086\3\u0086\3\u0086\3\u0086\7\u0086\u055c\n\u0086\f\u0086"+
		"\16\u0086\u055f\13\u0086\3\u0086\3\u0086\3\u0086\3\u0086\5\u0086\u0565"+
		"\n\u0086\3\u0087\3\u0087\3\u0087\3\u0087\7\u0087\u056b\n\u0087\f\u0087"+
		"\16\u0087\u056e\13\u0087\3\u0087\3\u0087\3\u0087\3\u0087\5\u0087\u0574"+
		"\n\u0087\3\u0088\3\u0088\3\u0088\3\u0088\3\u0089\3\u0089\3\u0089\3\u0089"+
		"\3\u0089\3\u0089\3\u0089\5\u0089\u0581\n\u0089\3\u008a\3\u008a\3\u008a"+
		"\3\u008b\3\u008b\3\u008b\3\u008c\5\u008c\u058a\n\u008c\3\u008c\3\u008c"+
		"\3\u008d\3\u008d\3\u008e\3\u008e\3\u008e\7\u008e\u0593\n\u008e\f\u008e"+
		"\16\u008e\u0596\13\u008e\3\u008f\3\u008f\3\u008f\5\u008f\u059b\n\u008f"+
		"\3\u008f\2\4(*\u0090\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60"+
		"\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086"+
		"\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e"+
		"\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6"+
		"\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce"+
		"\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6"+
		"\u00e8\u00ea\u00ec\u00ee\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe"+
		"\u0100\u0102\u0104\u0106\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116"+
		"\u0118\u011a\u011c\2\20\4\2\f\f\25\25\4\2\36\36--\3\2v{\3\2\u0082\u0083"+
		"\4\2rr\u0084\u0084\4\2,,PP\5\2rruu\u0082\u0082\3\2^_\5\2[[]]aa\3\2\25"+
		"\26\4\2\60\60LL\3\2\u0088\u0089\3\2\u008a\u008b\t\2\5\16\20\36!\668;>"+
		"RXh\u008c\u008c\u05ee\2\u011e\3\2\2\2\4\u0131\3\2\2\2\6\u0134\3\2\2\2"+
		"\b\u0138\3\2\2\2\n\u0143\3\2\2\2\f\u0146\3\2\2\2\16\u0149\3\2\2\2\20\u014b"+
		"\3\2\2\2\22\u0159\3\2\2\2\24\u016e\3\2\2\2\26\u0172\3\2\2\2\30\u0175\3"+
		"\2\2\2\32\u0187\3\2\2\2\34\u01ab\3\2\2\2\36\u01b2\3\2\2\2 \u01b4\3\2\2"+
		"\2\"\u01c2\3\2\2\2$\u01c8\3\2\2\2&\u01cb\3\2\2\2(\u01ce\3\2\2\2*\u01d9"+
		"\3\2\2\2,\u01e5\3\2\2\2.\u01ec\3\2\2\2\60\u01ee\3\2\2\2\62\u01f1\3\2\2"+
		"\2\64\u020b\3\2\2\2\66\u0214\3\2\2\28\u021c\3\2\2\2:\u021e\3\2\2\2<\u0226"+
		"\3\2\2\2>\u0231\3\2\2\2@\u0233\3\2\2\2B\u023b\3\2\2\2D\u0245\3\2\2\2F"+
		"\u0247\3\2\2\2H\u0250\3\2\2\2J\u0252\3\2\2\2L\u025c\3\2\2\2N\u026b\3\2"+
		"\2\2P\u026d\3\2\2\2R\u0278\3\2\2\2T\u027a\3\2\2\2V\u027e\3\2\2\2X\u029d"+
		"\3\2\2\2Z\u029f\3\2\2\2\\\u02ad\3\2\2\2^\u02c2\3\2\2\2`\u02c9\3\2\2\2"+
		"b\u02cd\3\2\2\2d\u02e0\3\2\2\2f\u02e2\3\2\2\2h\u02ee\3\2\2\2j\u02fe\3"+
		"\2\2\2l\u0300\3\2\2\2n\u0308\3\2\2\2p\u030b\3\2\2\2r\u0310\3\2\2\2t\u0315"+
		"\3\2\2\2v\u0317\3\2\2\2x\u0319\3\2\2\2z\u031b\3\2\2\2|\u0327\3\2\2\2~"+
		"\u0329\3\2\2\2\u0080\u032b\3\2\2\2\u0082\u0331\3\2\2\2\u0084\u0337\3\2"+
		"\2\2\u0086\u0339\3\2\2\2\u0088\u033b\3\2\2\2\u008a\u033d\3\2\2\2\u008c"+
		"\u033f\3\2\2\2\u008e\u0347\3\2\2\2\u0090\u0358\3\2\2\2\u0092\u035c\3\2"+
		"\2\2\u0094\u0368\3\2\2\2\u0096\u037f\3\2\2\2\u0098\u0381\3\2\2\2\u009a"+
		"\u0389\3\2\2\2\u009c\u038d\3\2\2\2\u009e\u0391\3\2\2\2\u00a0\u0395\3\2"+
		"\2\2\u00a2\u039c\3\2\2\2\u00a4\u039e\3\2\2\2\u00a6\u03b1\3\2\2\2\u00a8"+
		"\u03ba\3\2\2\2\u00aa\u03bd\3\2\2\2\u00ac\u03c6\3\2\2\2\u00ae\u03ce\3\2"+
		"\2\2\u00b0\u03e1\3\2\2\2\u00b2\u03e3\3\2\2\2\u00b4\u03eb\3\2\2\2\u00b6"+
		"\u0402\3\2\2\2\u00b8\u0404\3\2\2\2\u00ba\u0410\3\2\2\2\u00bc\u0422\3\2"+
		"\2\2\u00be\u043c\3\2\2\2\u00c0\u043e\3\2\2\2\u00c2\u0445\3\2\2\2\u00c4"+
		"\u0462\3\2\2\2\u00c6\u0464\3\2\2\2\u00c8\u046c\3\2\2\2\u00ca\u0470\3\2"+
		"\2\2\u00cc\u047d\3\2\2\2\u00ce\u047f\3\2\2\2\u00d0\u048b\3\2\2\2\u00d2"+
		"\u04a3\3\2\2\2\u00d4\u04ab\3\2\2\2\u00d6\u04be\3\2\2\2\u00d8\u04c7\3\2"+
		"\2\2\u00da\u04cb\3\2\2\2\u00dc\u04dd\3\2\2\2\u00de\u04e3\3\2\2\2\u00e0"+
		"\u04e7\3\2\2\2\u00e2\u04ed\3\2\2\2\u00e4\u04f5\3\2\2\2\u00e6\u04f7\3\2"+
		"\2\2\u00e8\u0505\3\2\2\2\u00ea\u0507\3\2\2\2\u00ec\u050a\3\2\2\2\u00ee"+
		"\u050e\3\2\2\2\u00f0\u0512\3\2\2\2\u00f2\u0515\3\2\2\2\u00f4\u0519\3\2"+
		"\2\2\u00f6\u051d\3\2\2\2\u00f8\u0523\3\2\2\2\u00fa\u0527\3\2\2\2\u00fc"+
		"\u052b\3\2\2\2\u00fe\u0535\3\2\2\2\u0100\u0537\3\2\2\2\u0102\u0541\3\2"+
		"\2\2\u0104\u0545\3\2\2\2\u0106\u0551\3\2\2\2\u0108\u0555\3\2\2\2\u010a"+
		"\u0564\3\2\2\2\u010c\u0573\3\2\2\2\u010e\u0575\3\2\2\2\u0110\u0580\3\2"+
		"\2\2\u0112\u0582\3\2\2\2\u0114\u0585\3\2\2\2\u0116\u0589\3\2\2\2\u0118"+
		"\u058d\3\2\2\2\u011a\u058f\3\2\2\2\u011c\u059a\3\2\2\2\u011e\u011f\5\4"+
		"\3\2\u011f\u0120\7\2\2\3\u0120\3\3\2\2\2\u0121\u0132\5\6\4\2\u0122\u0132"+
		"\5\u008eH\2\u0123\u0132\5\u00b4[\2\u0124\u0132\5\u00d6l\2\u0125\u0132"+
		"\5\u00d8m\2\u0126\u0132\5\u00ceh\2\u0127\u0132\5\u00c2b\2\u0128\u0132"+
		"\5\u00dep\2\u0129\u0132\5\u00dco\2\u012a\u0132\5\u00a0Q\2\u012b\u0132"+
		"\5\u00dan\2\u012c\u0132\5\u00b2Z\2\u012d\u0132\5\u00e0q\2\u012e\u0132"+
		"\5\u00e2r\2\u012f\u0132\5\u00d0i\2\u0130\u0132\5\u00d4k\2\u0131\u0121"+
		"\3\2\2\2\u0131\u0122\3\2\2\2\u0131\u0123\3\2\2\2\u0131\u0124\3\2\2\2\u0131"+
		"\u0125\3\2\2\2\u0131\u0126\3\2\2\2\u0131\u0127\3\2\2\2\u0131\u0128\3\2"+
		"\2\2\u0131\u0129\3\2\2\2\u0131\u012a\3\2\2\2\u0131\u012b\3\2\2\2\u0131"+
		"\u012c\3\2\2\2\u0131\u012d\3\2\2\2\u0131\u012e\3\2\2\2\u0131\u012f\3\2"+
		"\2\2\u0131\u0130\3\2\2\2\u0132\5\3\2\2\2\u0133\u0135\5\b\5\2\u0134\u0133"+
		"\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0136\3\2\2\2\u0136\u0137\5\20\t\2"+
		"\u0137\7\3\2\2\2\u0138\u0139\7\23\2\2\u0139\u013a\5\n\6\2\u013a\u0140"+
		"\7i\2\2\u013b\u013c\5\n\6\2\u013c\u013d\7i\2\2\u013d\u013f\3\2\2\2\u013e"+
		"\u013b\3\2\2\2\u013f\u0142\3\2\2\2\u0140\u013e\3\2\2\2\u0140\u0141\3\2"+
		"\2\2\u0141\t\3\2\2\2\u0142\u0140\3\2\2\2\u0143\u0144\5\f\7\2\u0144\u0145"+
		"\5d\63\2\u0145\13\3\2\2\2\u0146\u0147\7t\2\2\u0147\u0148\5\u011c\u008f"+
		"\2\u0148\r\3\2\2\2\u0149\u014a\5(\25\2\u014a\17\3\2\2\2\u014b\u014c\5"+
		"\30\r\2\u014c\u014e\5\22\n\2\u014d\u014f\5\26\f\2\u014e\u014d\3\2\2\2"+
		"\u014e\u014f\3\2\2\2\u014f\u0151\3\2\2\2\u0150\u0152\5 \21\2\u0151\u0150"+
		"\3\2\2\2\u0151\u0152\3\2\2\2\u0152\u0154\3\2\2\2\u0153\u0155\5$\23\2\u0154"+
		"\u0153\3\2\2\2\u0154\u0155\3\2\2\2\u0155\u0157\3\2\2\2\u0156\u0158\5&"+
		"\24\2\u0157\u0156\3\2\2\2\u0157\u0158\3\2\2\2\u0158\21\3\2\2\2\u0159\u015a"+
		"\7!\2\2\u015a\u015f\5\u008cG\2\u015b\u015d\7\13\2\2\u015c\u015b\3\2\2"+
		"\2\u015c\u015d\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u0160\5\24\13\2\u015f"+
		"\u015c\3\2\2\2\u015f\u0160\3\2\2\2\u0160\u016a\3\2\2\2\u0161\u0162\7j"+
		"\2\2\u0162\u0164\5\16\b\2\u0163\u0165\7\13\2\2\u0164\u0163\3\2\2\2\u0164"+
		"\u0165\3\2\2\2\u0165\u0166\3\2\2\2\u0166\u0167\5\f\7\2\u0167\u0169\3\2"+
		"\2\2\u0168\u0161\3\2\2\2\u0169\u016c\3\2\2\2\u016a\u0168\3\2\2\2\u016a"+
		"\u016b\3\2\2\2\u016b\23\3\2\2\2\u016c\u016a\3\2\2\2\u016d\u016f\7t\2\2"+
		"\u016e\u016d\3\2\2\2\u016e\u016f\3\2\2\2\u016f\u0170\3\2\2\2\u0170\u0171"+
		"\5\u011c\u008f\2\u0171\25\3\2\2\2\u0172\u0173\7R\2\2\u0173\u0174\5\16"+
		"\b\2\u0174\27\3\2\2\2\u0175\u0177\7B\2\2\u0176\u0178\5\32\16\2\u0177\u0176"+
		"\3\2\2\2\u0177\u0178\3\2\2\2\u0178\u0185\3\2\2\2\u0179\u0186\7r\2\2\u017a"+
		"\u017b\5\16\b\2\u017b\u0182\5\36\20\2\u017c\u017d\7j\2\2\u017d\u017e\5"+
		"\16\b\2\u017e\u017f\5\36\20\2\u017f\u0181\3\2\2\2\u0180\u017c\3\2\2\2"+
		"\u0181\u0184\3\2\2\2\u0182\u0180\3\2\2\2\u0182\u0183\3\2\2\2\u0183\u0186"+
		"\3\2\2\2\u0184\u0182\3\2\2\2\u0185\u0179\3\2\2\2\u0185\u017a\3\2\2\2\u0186"+
		"\31\3\2\2\2\u0187\u018b\7\3\2\2\u0188\u018a\5\34\17\2\u0189\u0188\3\2"+
		"\2\2\u018a\u018d\3\2\2\2\u018b\u0189\3\2\2\2\u018b\u018c\3\2\2\2\u018c"+
		"\u018e\3\2\2\2\u018d\u018b\3\2\2\2\u018e\u018f\7\4\2\2\u018f\33\3\2\2"+
		"\2\u0190\u0191\7<\2\2\u0191\u0192\7l\2\2\u0192\u0196\5\u008cG\2\u0193"+
		"\u0195\5\u00b6\\\2\u0194\u0193\3\2\2\2\u0195\u0198\3\2\2\2\u0196\u0194"+
		"\3\2\2\2\u0196\u0197\3\2\2\2\u0197\u0199\3\2\2\2\u0198\u0196\3\2\2\2\u0199"+
		"\u019a\7m\2\2\u019a\u01ac\3\2\2\2\u019b\u019c\7\37\2\2\u019c\u019d\7l"+
		"\2\2\u019d\u019e\5\u008cG\2\u019e\u019f\5\u00b6\\\2\u019f\u01a0\7m\2\2"+
		"\u01a0\u01ac\3\2\2\2\u01a1\u01a2\7=\2\2\u01a2\u01a3\7l\2\2\u01a3\u01a4"+
		"\5\u008cG\2\u01a4\u01a5\7m\2\2\u01a5\u01ac\3\2\2\2\u01a6\u01a7\7 \2\2"+
		"\u01a7\u01a8\7l\2\2\u01a8\u01a9\5\u008cG\2\u01a9\u01aa\7m\2\2\u01aa\u01ac"+
		"\3\2\2\2\u01ab\u0190\3\2\2\2\u01ab\u019b\3\2\2\2\u01ab\u01a1\3\2\2\2\u01ab"+
		"\u01a6\3\2\2\2\u01ac\u01ae\3\2\2\2\u01ad\u01af\7\u008b\2\2\u01ae\u01ad"+
		"\3\2\2\2\u01ae\u01af\3\2\2\2\u01af\35\3\2\2\2\u01b0\u01b1\7\13\2\2\u01b1"+
		"\u01b3\5\u011c\u008f\2\u01b2\u01b0\3\2\2\2\u01b2\u01b3\3\2\2\2\u01b3\37"+
		"\3\2\2\2\u01b4\u01b5\79\2\2\u01b5\u01b6\7\r\2\2\u01b6\u01b7\5\16\b\2\u01b7"+
		"\u01be\5\"\22\2\u01b8\u01b9\7j\2\2\u01b9\u01ba\5\16\b\2\u01ba\u01bb\5"+
		"\"\22\2\u01bb\u01bd\3\2\2\2\u01bc\u01b8\3\2\2\2\u01bd\u01c0\3\2\2\2\u01be"+
		"\u01bc\3\2\2\2\u01be\u01bf\3\2\2\2\u01bf!\3\2\2\2\u01c0\u01be\3\2\2\2"+
		"\u01c1\u01c3\t\2\2\2\u01c2\u01c1\3\2\2\2\u01c2\u01c3\3\2\2\2\u01c3\u01c6"+
		"\3\2\2\2\u01c4\u01c5\7\63\2\2\u01c5\u01c7\t\3\2\2\u01c6\u01c4\3\2\2\2"+
		"\u01c6\u01c7\3\2\2\2\u01c7#\3\2\2\2\u01c8\u01c9\7/\2\2\u01c9\u01ca\5:"+
		"\36\2\u01ca%\3\2\2\2\u01cb\u01cc\7\64\2\2\u01cc\u01cd\5:\36\2\u01cd\'"+
		"\3\2\2\2\u01ce\u01cf\b\25\1\2\u01cf\u01d0\5*\26\2\u01d0\u01d6\3\2\2\2"+
		"\u01d1\u01d2\f\3\2\2\u01d2\u01d3\78\2\2\u01d3\u01d5\5*\26\2\u01d4\u01d1"+
		"\3\2\2\2\u01d5\u01d8\3\2\2\2\u01d6\u01d4\3\2\2\2\u01d6\u01d7\3\2\2\2\u01d7"+
		")\3\2\2\2\u01d8\u01d6\3\2\2\2\u01d9\u01da\b\26\1\2\u01da\u01db\5,\27\2"+
		"\u01db\u01e1\3\2\2\2\u01dc\u01dd\f\3\2\2\u01dd\u01de\7\n\2\2\u01de\u01e0"+
		"\5,\27\2\u01df\u01dc\3\2\2\2\u01e0\u01e3\3\2\2\2\u01e1\u01df\3\2\2\2\u01e1"+
		"\u01e2\3\2\2\2\u01e2+\3\2\2\2\u01e3\u01e1\3\2\2\2\u01e4\u01e6\7\62\2\2"+
		"\u01e5\u01e4\3\2\2\2\u01e5\u01e6\3\2\2\2\u01e6\u01e7\3\2\2\2\u01e7\u01e8"+
		"\5.\30\2\u01e8-\3\2\2\2\u01e9\u01ed\5\64\33\2\u01ea\u01ed\5\60\31\2\u01eb"+
		"\u01ed\5\62\32\2\u01ec\u01e9\3\2\2\2\u01ec\u01ea\3\2\2\2\u01ec\u01eb\3"+
		"\2\2\2\u01ed/\3\2\2\2\u01ee\u01ef\7\35\2\2\u01ef\u01f0\5:\36\2\u01f0\61"+
		"\3\2\2\2\u01f1\u01f2\5:\36\2\u01f2\u01f4\7(\2\2\u01f3\u01f5\7\62\2\2\u01f4"+
		"\u01f3\3\2\2\2\u01f4\u01f5\3\2\2\2\u01f5\u01f6\3\2\2\2\u01f6\u01f8\7\65"+
		"\2\2\u01f7\u01f9\7K\2\2\u01f8\u01f7\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9"+
		"\u01fa\3\2\2\2\u01fa\u01fc\7l\2\2\u01fb\u01fd\7\67\2\2\u01fc\u01fb\3\2"+
		"\2\2\u01fc\u01fd\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u0206\5b\62\2\u01ff"+
		"\u0201\7j\2\2\u0200\u0202\7\67\2\2\u0201\u0200\3\2\2\2\u0201\u0202\3\2"+
		"\2\2\u0202\u0203\3\2\2\2\u0203\u0205\5b\62\2\u0204\u01ff\3\2\2\2\u0205"+
		"\u0208\3\2\2\2\u0206\u0204\3\2\2\2\u0206\u0207\3\2\2\2\u0207\u0209\3\2"+
		"\2\2\u0208\u0206\3\2\2\2\u0209\u020a\7m\2\2\u020a\63\3\2\2\2\u020b\u0212"+
		"\5:\36\2\u020c\u020f\5\66\34\2\u020d\u020f\58\35\2\u020e\u020c\3\2\2\2"+
		"\u020e\u020d\3\2\2\2\u020f\u0210\3\2\2\2\u0210\u0211\5:\36\2\u0211\u0213"+
		"\3\2\2\2\u0212\u020e\3\2\2\2\u0212\u0213\3\2\2\2\u0213\65\3\2\2\2\u0214"+
		"\u0215\t\4\2\2\u0215\67\3\2\2\2\u0216\u021d\7\u0080\2\2\u0217\u021d\7"+
		"\u0081\2\2\u0218\u021d\7~\2\2\u0219\u021d\7\177\2\2\u021a\u021d\7|\2\2"+
		"\u021b\u021d\7}\2\2\u021c\u0216\3\2\2\2\u021c\u0217\3\2\2\2\u021c\u0218"+
		"\3\2\2\2\u021c\u0219\3\2\2\2\u021c\u021a\3\2\2\2\u021c\u021b\3\2\2\2\u021d"+
		"9\3\2\2\2\u021e\u0223\5<\37\2\u021f\u0220\t\5\2\2\u0220\u0222\5<\37\2"+
		"\u0221\u021f\3\2\2\2\u0222\u0225\3\2\2\2\u0223\u0221\3\2\2\2\u0223\u0224"+
		"\3\2\2\2\u0224;\3\2\2\2\u0225\u0223\3\2\2\2\u0226\u022b\5> \2\u0227\u0228"+
		"\t\6\2\2\u0228\u022a\5> \2\u0229\u0227\3\2\2\2\u022a\u022d\3\2\2\2\u022b"+
		"\u0229\3\2\2\2\u022b\u022c\3\2\2\2\u022c=\3\2\2\2\u022d\u022b\3\2\2\2"+
		"\u022e\u0232\5@!\2\u022f\u0230\t\5\2\2\u0230\u0232\5> \2\u0231\u022e\3"+
		"\2\2\2\u0231\u022f\3\2\2\2\u0232?\3\2\2\2\u0233\u0238\5N(\2\u0234\u0237"+
		"\5B\"\2\u0235\u0237\5H%\2\u0236\u0234\3\2\2\2\u0236\u0235\3\2\2\2\u0237"+
		"\u023a\3\2\2\2\u0238\u0236\3\2\2\2\u0238\u0239\3\2\2\2\u0239A\3\2\2\2"+
		"\u023a\u0238\3\2\2\2\u023b\u023e\7s\2\2\u023c\u023f\5F$\2\u023d\u023f"+
		"\5D#\2\u023e\u023c\3\2\2\2\u023e\u023d\3\2\2\2\u023fC\3\2\2\2\u0240\u0246"+
		"\5\u011c\u008f\2\u0241\u0246\5\u0118\u008d\2\u0242\u0246\5T+\2\u0243\u0246"+
		"\5`\61\2\u0244\u0246\5Z.\2\u0245\u0240\3\2\2\2\u0245\u0241\3\2\2\2\u0245"+
		"\u0242\3\2\2\2\u0245\u0243\3\2\2\2\u0245\u0244\3\2\2\2\u0246E\3\2\2\2"+
		"\u0247\u0248\t\7\2\2\u0248\u024a\7l\2\2\u0249\u024b\5\16\b\2\u024a\u0249"+
		"\3\2\2\2\u024a\u024b\3\2\2\2\u024b\u024c\3\2\2\2\u024c\u024d\7m\2\2\u024d"+
		"G\3\2\2\2\u024e\u0251\5L\'\2\u024f\u0251\5J&\2\u0250\u024e\3\2\2\2\u0250"+
		"\u024f\3\2\2\2\u0251I\3\2\2\2\u0252\u0254\7n\2\2\u0253\u0255\5\16\b\2"+
		"\u0254\u0253\3\2\2\2\u0254\u0255\3\2\2\2\u0255\u0256\3\2\2\2\u0256\u0258"+
		"\7k\2\2\u0257\u0259\5\16\b\2\u0258\u0257\3\2\2\2\u0258\u0259\3\2\2\2\u0259"+
		"\u025a\3\2\2\2\u025a\u025b\7o\2\2\u025bK\3\2\2\2\u025c\u025e\7n\2\2\u025d"+
		"\u025f\5\16\b\2\u025e\u025d\3\2\2\2\u025e\u025f\3\2\2\2\u025f\u0260\3"+
		"\2\2\2\u0260\u0261\7o\2\2\u0261M\3\2\2\2\u0262\u026c\5R*\2\u0263\u026c"+
		"\5P)\2\u0264\u026c\5T+\2\u0265\u026c\5V,\2\u0266\u026c\5X-\2\u0267\u026c"+
		"\5Z.\2\u0268\u026c\5\\/\2\u0269\u026c\5^\60\2\u026a\u026c\5`\61\2\u026b"+
		"\u0262\3\2\2\2\u026b\u0263\3\2\2\2\u026b\u0264\3\2\2\2\u026b\u0265\3\2"+
		"\2\2\u026b\u0266\3\2\2\2\u026b\u0267\3\2\2\2\u026b\u0268\3\2\2\2\u026b"+
		"\u0269\3\2\2\2\u026b\u026a\3\2\2\2\u026cO\3\2\2\2\u026d\u0270\5\u011c"+
		"\u008f\2\u026e\u026f\7s\2\2\u026f\u0271\5\u011c\u008f\2\u0270\u026e\3"+
		"\2\2\2\u0270\u0271\3\2\2\2\u0271Q\3\2\2\2\u0272\u0279\7\u0088\2\2\u0273"+
		"\u0279\7\u0089\2\2\u0274\u0279\5\u0118\u008d\2\u0275\u0279\7\u0087\2\2"+
		"\u0276\u0279\7\u0086\2\2\u0277\u0279\7\u0085\2\2\u0278\u0272\3\2\2\2\u0278"+
		"\u0273\3\2\2\2\u0278\u0274\3\2\2\2\u0278\u0275\3\2\2\2\u0278\u0276\3\2"+
		"\2\2\u0278\u0277\3\2\2\2\u0279S\3\2\2\2\u027a\u027c\7t\2\2\u027b\u027d"+
		"\5\u011c\u008f\2\u027c\u027b\3\2\2\2\u027c\u027d\3\2\2\2\u027dU\3\2\2"+
		"\2\u027e\u0280\7n\2\2\u027f\u0281\5\16\b\2\u0280\u027f\3\2\2\2\u0280\u0281"+
		"\3\2\2\2\u0281\u0286\3\2\2\2\u0282\u0283\7j\2\2\u0283\u0285\5\16\b\2\u0284"+
		"\u0282\3\2\2\2\u0285\u0288\3\2\2\2\u0286\u0284\3\2\2\2\u0286\u0287\3\2"+
		"\2\2\u0287\u0289\3\2\2\2\u0288\u0286\3\2\2\2\u0289\u028a\7o\2\2\u028a"+
		"W\3\2\2\2\u028b\u028c\7p\2\2\u028c\u028d\5\16\b\2\u028d\u028e\7k\2\2\u028e"+
		"\u0296\5\16\b\2\u028f\u0290\7j\2\2\u0290\u0291\5\16\b\2\u0291\u0292\7"+
		"k\2\2\u0292\u0293\5\16\b\2\u0293\u0295\3\2\2\2\u0294\u028f\3\2\2\2\u0295"+
		"\u0298\3\2\2\2\u0296\u0294\3\2\2\2\u0296\u0297\3\2\2\2\u0297\u0299\3\2"+
		"\2\2\u0298\u0296\3\2\2\2\u0299\u029a\7q\2\2\u029a\u029e\3\2\2\2\u029b"+
		"\u029c\7p\2\2\u029c\u029e\7q\2\2\u029d\u028b\3\2\2\2\u029d\u029b\3\2\2"+
		"\2\u029eY\3\2\2\2\u029f\u02a0\5\u011c\u008f\2\u02a0\u02a9\7l\2\2\u02a1"+
		"\u02a6\5\16\b\2\u02a2\u02a3\7j\2\2\u02a3\u02a5\5\16\b\2\u02a4\u02a2\3"+
		"\2\2\2\u02a5\u02a8\3\2\2\2\u02a6\u02a4\3\2\2\2\u02a6\u02a7\3\2\2\2\u02a7"+
		"\u02aa\3\2\2\2\u02a8\u02a6\3\2\2\2\u02a9\u02a1\3\2\2\2\u02a9\u02aa\3\2"+
		"\2\2\u02aa\u02ab\3\2\2\2\u02ab\u02ac\7m\2\2\u02ac[\3\2\2\2\u02ad\u02ae"+
		"\7\16\2\2\u02ae\u02af\7Q\2\2\u02af\u02b0\5\16\b\2\u02b0\u02b1\7G\2\2\u02b1"+
		"\u02b9\5\16\b\2\u02b2\u02b3\7Q\2\2\u02b3\u02b4\5\16\b\2\u02b4\u02b5\7"+
		"G\2\2\u02b5\u02b6\5\16\b\2\u02b6\u02b8\3\2\2\2\u02b7\u02b2\3\2\2\2\u02b8"+
		"\u02bb\3\2\2\2\u02b9\u02b7\3\2\2\2\u02b9\u02ba\3\2\2\2\u02ba\u02be\3\2"+
		"\2\2\u02bb\u02b9\3\2\2\2\u02bc\u02bd\7\31\2\2\u02bd\u02bf\5\16\b\2\u02be"+
		"\u02bc\3\2\2\2\u02be\u02bf\3\2\2\2\u02bf\u02c0\3\2\2\2\u02c0\u02c1\7\32"+
		"\2\2\u02c1]\3\2\2\2\u02c2\u02c3\7\20\2\2\u02c3\u02c4\7l\2\2\u02c4\u02c5"+
		"\5\16\b\2\u02c5\u02c6\7\13\2\2\u02c6\u02c7\5b\62\2\u02c7\u02c8\7m\2\2"+
		"\u02c8_\3\2\2\2\u02c9\u02ca\7l\2\2\u02ca\u02cb\5\16\b\2\u02cb\u02cc\7"+
		"m\2\2\u02cca\3\2\2\2\u02cd\u02cf\5d\63\2\u02ce\u02d0\t\b\2\2\u02cf\u02ce"+
		"\3\2\2\2\u02cf\u02d0\3\2\2\2\u02d0c\3\2\2\2\u02d1\u02e1\5\u0080A\2\u02d2"+
		"\u02e1\5r:\2\u02d3\u02e1\5~@\2\u02d4\u02e1\5|?\2\u02d5\u02e1\5x=\2\u02d6"+
		"\u02e1\5t;\2\u02d7\u02e1\5v<\2\u02d8\u02e1\5p9\2\u02d9\u02e1\5f\64\2\u02da"+
		"\u02e1\5z>\2\u02db\u02e1\5\u0082B\2\u02dc\u02e1\5\u0084C\2\u02dd\u02e1"+
		"\5\u0086D\2\u02de\u02e1\5\u0088E\2\u02df\u02e1\5\u008aF\2\u02e0\u02d1"+
		"\3\2\2\2\u02e0\u02d2\3\2\2\2\u02e0\u02d3\3\2\2\2\u02e0\u02d4\3\2\2\2\u02e0"+
		"\u02d5\3\2\2\2\u02e0\u02d6\3\2\2\2\u02e0\u02d7\3\2\2\2\u02e0\u02d8\3\2"+
		"\2\2\u02e0\u02d9\3\2\2\2\u02e0\u02da\3\2\2\2\u02e0\u02db\3\2\2\2\u02e0"+
		"\u02dc\3\2\2\2\u02e0\u02dd\3\2\2\2\u02e0\u02de\3\2\2\2\u02e0\u02df\3\2"+
		"\2\2\u02e1e\3\2\2\2\u02e2\u02e3\7b\2\2\u02e3\u02e4\7l\2\2\u02e4\u02e9"+
		"\5h\65\2\u02e5\u02e6\7j\2\2\u02e6\u02e8\5h\65\2\u02e7\u02e5\3\2\2\2\u02e8"+
		"\u02eb\3\2\2\2\u02e9\u02e7\3\2\2\2\u02e9\u02ea\3\2\2\2\u02ea\u02ec\3\2"+
		"\2\2\u02eb\u02e9\3\2\2\2\u02ec\u02ed\7m\2\2\u02edg\3\2\2\2\u02ee\u02ef"+
		"\5\u011c\u008f\2\u02ef\u02f1\5d\63\2\u02f0\u02f2\5j\66\2\u02f1\u02f0\3"+
		"\2\2\2\u02f1\u02f2\3\2\2\2\u02f2\u02f4\3\2\2\2\u02f3\u02f5\5\u0112\u008a"+
		"\2\u02f4\u02f3\3\2\2\2\u02f4\u02f5\3\2\2\2\u02f5i\3\2\2\2\u02f6\u02f8"+
		"\5l\67\2\u02f7\u02f9\5n8\2\u02f8\u02f7\3\2\2\2\u02f8\u02f9\3\2\2\2\u02f9"+
		"\u02ff\3\2\2\2\u02fa\u02fc\5n8\2\u02fb\u02fd\5l\67\2\u02fc\u02fb\3\2\2"+
		"\2\u02fc\u02fd\3\2\2\2\u02fd\u02ff\3\2\2\2\u02fe\u02f6\3\2\2\2\u02fe\u02fa"+
		"\3\2\2\2\u02ffk\3\2\2\2\u0300\u0306\7\24\2\2\u0301\u0307\5\u0116\u008c"+
		"\2\u0302\u0307\5\u0118\u008d\2\u0303\u0307\7\u0087\2\2\u0304\u0307\7\u0086"+
		"\2\2\u0305\u0307\5\u011c\u008f\2\u0306\u0301\3\2\2\2\u0306\u0302\3\2\2"+
		"\2\u0306\u0303\3\2\2\2\u0306\u0304\3\2\2\2\u0306\u0305\3\2\2\2\u0307m"+
		"\3\2\2\2\u0308\u0309\7\62\2\2\u0309\u030a\7\u0085\2\2\u030ao\3\2\2\2\u030b"+
		"\u030c\7`\2\2\u030c\u030d\7l\2\2\u030d\u030e\5d\63\2\u030e\u030f\7m\2"+
		"\2\u030fq\3\2\2\2\u0310\u0311\7X\2\2\u0311\u0312\7l\2\2\u0312\u0313\5"+
		"d\63\2\u0313\u0314\7m\2\2\u0314s\3\2\2\2\u0315\u0316\t\t\2\2\u0316u\3"+
		"\2\2\2\u0317\u0318\7)\2\2\u0318w\3\2\2\2\u0319\u031a\t\n\2\2\u031ay\3"+
		"\2\2\2\u031b\u031c\7c\2\2\u031c{\3\2\2\2\u031d\u031e\7\\\2\2\u031e\u031f"+
		"\7l\2\2\u031f\u0320\5\u011a\u008e\2\u0320\u0321\7m\2\2\u0321\u0328\3\2"+
		"\2\2\u0322\u0323\7\\\2\2\u0323\u0324\7l\2\2\u0324\u0325\5\u011a\u008e"+
		"\2\u0325\u0326\b?\1\2\u0326\u0328\3\2\2\2\u0327\u031d\3\2\2\2\u0327\u0322"+
		"\3\2\2\2\u0328}\3\2\2\2\u0329\u032a\7Z\2\2\u032a\177\3\2\2\2\u032b\u032f"+
		"\7Y\2\2\u032c\u032d\7l\2\2\u032d\u032e\7\u0088\2\2\u032e\u0330\7m\2\2"+
		"\u032f\u032c\3\2\2\2\u032f\u0330\3\2\2\2\u0330\u0081\3\2\2\2\u0331\u0335"+
		"\7d\2\2\u0332\u0333\7l\2\2\u0333\u0334\7\u0088\2\2\u0334\u0336\7m\2\2"+
		"\u0335\u0332\3\2\2\2\u0335\u0336\3\2\2\2\u0336\u0083\3\2\2\2\u0337\u0338"+
		"\7e\2\2\u0338\u0085\3\2\2\2\u0339\u033a\7f\2\2\u033a\u0087\3\2\2\2\u033b"+
		"\u033c\7g\2\2\u033c\u0089\3\2\2\2\u033d\u033e\7h\2\2\u033e\u008b\3\2\2"+
		"\2\u033f\u0344\5\u011c\u008f\2\u0340\u0341\7s\2\2\u0341\u0343\5\u011c"+
		"\u008f\2\u0342\u0340\3\2\2\2\u0343\u0346\3\2\2\2\u0344\u0342\3\2\2\2\u0344"+
		"\u0345\3\2\2\2\u0345\u008d\3\2\2\2\u0346\u0344\3\2\2\2\u0347\u0348\7\22"+
		"\2\2\u0348\u034c\7E\2\2\u0349\u034a\7%\2\2\u034a\u034b\7\62\2\2\u034b"+
		"\u034d\7\35\2\2\u034c\u0349\3\2\2\2\u034c\u034d\3\2\2\2\u034d\u034e\3"+
		"\2\2\2\u034e\u0350\5\u0090I\2\u034f\u0351\5\u0112\u008a\2\u0350\u034f"+
		"\3\2\2\2\u0350\u0351\3\2\2\2\u0351\u0352\3\2\2\2\u0352\u0353\7l\2\2\u0353"+
		"\u0354\5\u0092J\2\u0354\u0356\7m\2\2\u0355\u0357\5\u009eP\2\u0356\u0355"+
		"\3\2\2\2\u0356\u0357\3\2\2\2\u0357\u008f\3\2\2\2\u0358\u0359\5\u008cG"+
		"\2\u0359\u0091\3\2\2\2\u035a\u035d\5h\65\2\u035b\u035d\5\u0094K\2\u035c"+
		"\u035a\3\2\2\2\u035c\u035b\3\2\2\2\u035d\u0365\3\2\2\2\u035e\u0361\7j"+
		"\2\2\u035f\u0362\5h\65\2\u0360\u0362\5\u0094K\2\u0361\u035f\3\2\2\2\u0361"+
		"\u0360\3\2\2\2\u0362\u0364\3\2\2\2\u0363\u035e\3\2\2\2\u0364\u0367\3\2"+
		"\2\2\u0365\u0363\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u0093\3\2\2\2\u0367"+
		"\u0365\3\2\2\2\u0368\u0369\7>\2\2\u0369\u036a\7*\2\2\u036a\u036f\7l\2"+
		"\2\u036b\u036d\5\u0096L\2\u036c\u036e\7j\2\2\u036d\u036c\3\2\2\2\u036d"+
		"\u036e\3\2\2\2\u036e\u0370\3\2\2\2\u036f\u036b\3\2\2\2\u036f\u0370\3\2"+
		"\2\2\u0370\u0372\3\2\2\2\u0371\u0373\5\u0098M\2\u0372\u0371\3\2\2\2\u0372"+
		"\u0373\3\2\2\2\u0373\u0374\3\2\2\2\u0374\u0375\7m\2\2\u0375\u0095\3\2"+
		"\2\2\u0376\u0377\7C\2\2\u0377\u0378\7l\2\2\u0378\u0379\5\u0098M\2\u0379"+
		"\u037a\7m\2\2\u037a\u0380\3\2\2\2\u037b\u037c\7l\2\2\u037c\u037d\5\u0098"+
		"M\2\u037d\u037e\bL\1\2\u037e\u0380\3\2\2\2\u037f\u0376\3\2\2\2\u037f\u037b"+
		"\3\2\2\2\u0380\u0097\3\2\2\2\u0381\u0386\5\u009aN\2\u0382\u0383\7j\2\2"+
		"\u0383\u0385\5\u009aN\2\u0384\u0382\3\2\2\2\u0385\u0388\3\2\2\2\u0386"+
		"\u0384\3\2\2\2\u0386\u0387\3\2\2\2\u0387\u0099\3\2\2\2\u0388\u0386\3\2"+
		"\2\2\u0389\u038b\5\u011c\u008f\2\u038a\u038c\5\u009cO\2\u038b\u038a\3"+
		"\2\2\2\u038b\u038c\3\2\2\2\u038c\u009b\3\2\2\2\u038d\u038e\7l\2\2\u038e"+
		"\u038f\7\u0088\2\2\u038f\u0390\7m\2\2\u0390\u009d\3\2\2\2\u0391\u0392"+
		"\7O\2\2\u0392\u0393\7J\2\2\u0393\u0394\5\u0114\u008b\2\u0394\u009f\3\2"+
		"\2\2\u0395\u0396\7\t\2\2\u0396\u0397\7E\2\2\u0397\u0398\5\u0090I\2\u0398"+
		"\u0399\5\u00a2R\2\u0399\u00a1\3\2\2\2\u039a\u039d\5\u00a4S\2\u039b\u039d"+
		"\5\u009eP\2\u039c\u039a\3\2\2\2\u039c\u039b\3\2\2\2\u039d\u00a3\3\2\2"+
		"\2\u039e\u03a2\7l\2\2\u039f\u03a3\5\u00a6T\2\u03a0\u03a3\5\u00a8U\2\u03a1"+
		"\u03a3\5\u00aaV\2\u03a2\u039f\3\2\2\2\u03a2\u03a0\3\2\2\2\u03a2\u03a1"+
		"\3\2\2\2\u03a3\u03ac\3\2\2\2\u03a4\u03a8\7j\2\2\u03a5\u03a9\5\u00a6T\2"+
		"\u03a6\u03a9\5\u00a8U\2\u03a7\u03a9\5\u00aaV\2\u03a8\u03a5\3\2\2\2\u03a8"+
		"\u03a6\3\2\2\2\u03a8\u03a7\3\2\2\2\u03a9\u03ab\3\2\2\2\u03aa\u03a4\3\2"+
		"\2\2\u03ab\u03ae\3\2\2\2\u03ac\u03aa\3\2\2\2\u03ac\u03ad\3\2\2\2\u03ad"+
		"\u03af\3\2\2\2\u03ae\u03ac\3\2\2\2\u03af\u03b0\7m\2\2\u03b0\u00a5\3\2"+
		"\2\2\u03b1\u03b2\7\6\2\2\u03b2\u03b3\5\u00acW\2\u03b3\u03b5\5d\63\2\u03b4"+
		"\u03b6\5j\66\2\u03b5\u03b4\3\2\2\2\u03b5\u03b6\3\2\2\2\u03b6\u03b8\3\2"+
		"\2\2\u03b7\u03b9\5\u0112\u008a\2\u03b8\u03b7\3\2\2\2\u03b8\u03b9\3\2\2"+
		"\2\u03b9\u00a7\3\2\2\2\u03ba\u03bb\7\27\2\2\u03bb\u03bc\5\u00acW\2\u03bc"+
		"\u00a9\3\2\2\2\u03bd\u03be\7\61\2\2\u03be\u03bf\5\u00acW\2\u03bf\u03c1"+
		"\5d\63\2\u03c0\u03c2\5j\66\2\u03c1\u03c0\3\2\2\2\u03c1\u03c2\3\2\2\2\u03c2"+
		"\u03c4\3\2\2\2\u03c3\u03c5\5\u0112\u008a\2\u03c4\u03c3\3\2\2\2\u03c4\u03c5"+
		"\3\2\2\2\u03c5\u00ab\3\2\2\2\u03c6\u03cb\5\u00aeX\2\u03c7\u03c8\7s\2\2"+
		"\u03c8\u03ca\5\u00b0Y\2\u03c9\u03c7\3\2\2\2\u03ca\u03cd\3\2\2\2\u03cb"+
		"\u03c9\3\2\2\2\u03cb\u03cc\3\2\2\2\u03cc\u00ad\3\2\2\2\u03cd\u03cb\3\2"+
		"\2\2\u03ce\u03d3\5\u011c\u008f\2\u03cf\u03d0\7n\2\2\u03d0\u03d2\7o\2\2"+
		"\u03d1\u03cf\3\2\2\2\u03d2\u03d5\3\2\2\2\u03d3\u03d1\3\2\2\2\u03d3\u03d4"+
		"\3\2\2\2\u03d4\u00af\3\2\2\2\u03d5\u03d3\3\2\2\2\u03d6\u03db\5\u011c\u008f"+
		"\2\u03d7\u03d8\7n\2\2\u03d8\u03da\7o\2\2\u03d9\u03d7\3\2\2\2\u03da\u03dd"+
		"\3\2\2\2\u03db\u03d9\3\2\2\2\u03db\u03dc\3\2\2\2\u03dc\u03e2\3\2\2\2\u03dd"+
		"\u03db\3\2\2\2\u03de\u03df\7P\2\2\u03df\u03e0\7l\2\2\u03e0\u03e2\7m\2"+
		"\2\u03e1\u03d6\3\2\2\2\u03e1\u03de\3\2\2\2\u03e2\u00b1\3\2\2\2\u03e3\u03e4"+
		"\7\27\2\2\u03e4\u03e7\7E\2\2\u03e5\u03e6\7%\2\2\u03e6\u03e8\7\35\2\2\u03e7"+
		"\u03e5\3\2\2\2\u03e7\u03e8\3\2\2\2\u03e8\u03e9\3\2\2\2\u03e9\u03ea\5\u008c"+
		"G\2\u03ea\u00b3\3\2\2\2\u03eb\u03ec\7\22\2\2\u03ec\u03f0\7&\2\2\u03ed"+
		"\u03ee\7%\2\2\u03ee\u03ef\7\62\2\2\u03ef\u03f1\7\35\2\2\u03f0\u03ed\3"+
		"\2\2\2\u03f0\u03f1\3\2\2\2\u03f1\u03f2\3\2\2\2\u03f2\u03f3\5\u00b6\\\2"+
		"\u03f3\u03f4\7\66\2\2\u03f4\u03fd\5\u0090I\2\u03f5\u03f6\7l\2\2\u03f6"+
		"\u03f7\5\u00b8]\2\u03f7\u03f8\7m\2\2\u03f8\u03fe\3\2\2\2\u03f9\u03fa\7"+
		"l\2\2\u03fa\u03fb\5\u00b8]\2\u03fb\u03fc\b[\1\2\u03fc\u03fe\3\2\2\2\u03fd"+
		"\u03f5\3\2\2\2\u03fd\u03f9\3\2\2\2\u03fe\u0400\3\2\2\2\u03ff\u0401\5\u0112"+
		"\u008a\2\u0400\u03ff\3\2\2\2\u0400\u0401\3\2\2\2\u0401\u00b5\3\2\2\2\u0402"+
		"\u0403\5\u011c\u008f\2\u0403\u00b7\3\2\2\2\u0404\u0409\5\u00ba^\2\u0405"+
		"\u0406\7j\2\2\u0406\u0408\5\u00ba^\2\u0407\u0405\3\2\2\2\u0408\u040b\3"+
		"\2\2\2\u0409\u0407\3\2\2\2\u0409\u040a\3\2\2\2\u040a\u00b9\3\2\2\2\u040b"+
		"\u0409\3\2\2\2\u040c\u0411\5\u008cG\2\u040d\u0411\5\u00bc_\2\u040e\u0411"+
		"\5\u00be`\2\u040f\u0411\5\u00c0a\2\u0410\u040c\3\2\2\2\u0410\u040d\3\2"+
		"\2\2\u0410\u040e\3\2\2\2\u0410\u040f\3\2\2\2\u0411\u00bb\3\2\2\2\u0412"+
		"\u0413\5\u008cG\2\u0413\u0414\7s\2\2\u0414\u0415\7,\2\2\u0415\u0416\7"+
		"l\2\2\u0416\u0417\7m\2\2\u0417\u0423\3\2\2\2\u0418\u0419\7+\2\2\u0419"+
		"\u041a\7l\2\2\u041a\u041b\5\u008cG\2\u041b\u041c\7m\2\2\u041c\u0423\3"+
		"\2\2\2\u041d\u041e\7,\2\2\u041e\u041f\7l\2\2\u041f\u0420\5\u008cG\2\u0420"+
		"\u0421\7m\2\2\u0421\u0423\3\2\2\2\u0422\u0412\3\2\2\2\u0422\u0418\3\2"+
		"\2\2\u0422\u041d\3\2\2\2\u0423\u00bd\3\2\2\2\u0424\u0425\5\u008cG\2\u0425"+
		"\u0426\7s\2\2\u0426\u0427\7P\2\2\u0427\u0428\7l\2\2\u0428\u042b\7m\2\2"+
		"\u0429\u042a\7s\2\2\u042a\u042c\5\u008cG\2\u042b\u0429\3\2\2\2\u042b\u042c"+
		"\3\2\2\2\u042c\u043d\3\2\2\2\u042d\u042e\5\u008cG\2\u042e\u042f\7n\2\2"+
		"\u042f\u0430\7o\2\2\u0430\u0437\3\2\2\2\u0431\u0432\7\30\2\2\u0432\u0433"+
		"\7l\2\2\u0433\u0434\5\u008cG\2\u0434\u0435\7m\2\2\u0435\u0437\3\2\2\2"+
		"\u0436\u042d\3\2\2\2\u0436\u0431\3\2\2\2\u0437\u043a\3\2\2\2\u0438\u0439"+
		"\7s\2\2\u0439\u043b\5\u008cG\2\u043a\u0438\3\2\2\2\u043a\u043b\3\2\2\2"+
		"\u043b\u043d\3\2\2\2\u043c\u0424\3\2\2\2\u043c\u0436\3\2\2\2\u043d\u00bf"+
		"\3\2\2\2\u043e\u043f\5\u008cG\2\u043f\u0440\7n\2\2\u0440\u0443\7o\2\2"+
		"\u0441\u0442\7s\2\2\u0442\u0444\5\u008cG\2\u0443\u0441\3\2\2\2\u0443\u0444"+
		"\3\2\2\2\u0444\u00c1\3\2\2\2\u0445\u0446\7\22\2\2\u0446\u0447\7\"\2\2"+
		"\u0447\u044b\7&\2\2\u0448\u0449\7%\2\2\u0449\u044a\7\62\2\2\u044a\u044c"+
		"\7\35\2\2\u044b\u0448\3\2\2\2\u044b\u044c\3\2\2\2\u044c\u044d\3\2\2\2"+
		"\u044d\u044e\5\u00b6\\\2\u044e\u044f\7\66\2\2\u044f\u0450\5\u0090I\2\u0450"+
		"\u0452\5\u00c4c\2\u0451\u0453\5\u00caf\2\u0452\u0451\3\2\2\2\u0452\u0453"+
		"\3\2\2\2\u0453\u0455\3\2\2\2\u0454\u0456\7:\2\2\u0455\u0454\3\2\2\2\u0455"+
		"\u0456\3\2\2\2\u0456\u0458\3\2\2\2\u0457\u0459\5\u0112\u008a\2\u0458\u0457"+
		"\3\2\2\2\u0458\u0459\3\2\2\2\u0459\u00c3\3\2\2\2\u045a\u045b\7l\2\2\u045b"+
		"\u045c\5\u00c6d\2\u045c\u045d\7m\2\2\u045d\u0463\3\2\2\2\u045e\u045f\7"+
		"l\2\2\u045f\u0460\5\u00c6d\2\u0460\u0461\bc\1\2\u0461\u0463\3\2\2\2\u0462"+
		"\u045a\3\2\2\2\u0462\u045e\3\2\2\2\u0463\u00c5\3\2\2\2\u0464\u0469\5\u00c8"+
		"e\2\u0465\u0466\7j\2\2\u0466\u0468\5\u00c8e\2\u0467\u0465\3\2\2\2\u0468"+
		"\u046b\3\2\2\2\u0469\u0467\3\2\2\2\u0469\u046a\3\2\2\2\u046a\u00c7\3\2"+
		"\2\2\u046b\u0469\3\2\2\2\u046c\u046e\5\u00ba^\2\u046d\u046f\5\u010a\u0086"+
		"\2\u046e\u046d\3\2\2\2\u046e\u046f\3\2\2\2\u046f\u00c9\3\2\2\2\u0470\u0474"+
		"\5\u00ccg\2\u0471\u0473\5\u00ccg\2\u0472\u0471\3\2\2\2\u0473\u0476\3\2"+
		"\2\2\u0474\u0472\3\2\2\2\u0474\u0475\3\2\2\2\u0475\u00cb\3\2\2\2\u0476"+
		"\u0474\3\2\2\2\u0477\u0478\7\33\2\2\u0478\u0479\7z\2\2\u0479\u047e\7\u0088"+
		"\2\2\u047a\u047b\7\34\2\2\u047b\u047c\7z\2\2\u047c\u047e\7\u0088\2\2\u047d"+
		"\u0477\3\2\2\2\u047d\u047a\3\2\2\2\u047e\u00cd\3\2\2\2\u047f\u0480\7\27"+
		"\2\2\u0480\u0483\7&\2\2\u0481\u0482\7%\2\2\u0482\u0484\7\35\2\2\u0483"+
		"\u0481\3\2\2\2\u0483\u0484\3\2\2\2\u0484\u0485\3\2\2\2\u0485\u0486\5\u00b6"+
		"\\\2\u0486\u0487\7\66\2\2\u0487\u0489\5\u008cG\2\u0488\u048a\7:\2\2\u0489"+
		"\u0488\3\2\2\2\u0489\u048a\3\2\2\2\u048a\u00cf\3\2\2\2\u048b\u048e\t\13"+
		"\2\2\u048c\u048d\7\13\2\2\u048d\u048f\7)\2\2\u048e\u048c\3\2\2\2\u048e"+
		"\u048f\3\2\2\2\u048f\u04a1\3\2\2\2\u0490\u0491\7E\2\2\u0491\u049a\5\u008c"+
		"G\2\u0492\u0493\7l\2\2\u0493\u0494\5\u00d2j\2\u0494\u0495\7m\2\2\u0495"+
		"\u049b\3\2\2\2\u0496\u0497\7l\2\2\u0497\u0498\5\u00d2j\2\u0498\u0499\b"+
		"i\1\2\u0499\u049b\3\2\2\2\u049a\u0492\3\2\2\2\u049a\u0496\3\2\2\2\u049a"+
		"\u049b\3\2\2\2\u049b\u04a2\3\2\2\2\u049c\u049d\7&\2\2\u049d\u049e\5\u00b6"+
		"\\\2\u049e\u049f\7\66\2\2\u049f\u04a0\5\u008cG\2\u04a0\u04a2\3\2\2\2\u04a1"+
		"\u0490\3\2\2\2\u04a1\u049c\3\2\2\2\u04a2\u00d1\3\2\2\2\u04a3\u04a8\5\u00ac"+
		"W\2\u04a4\u04a5\7j\2\2\u04a5\u04a7\5\u00acW\2\u04a6\u04a4\3\2\2\2\u04a7"+
		"\u04aa\3\2\2\2\u04a8\u04a6\3\2\2\2\u04a8\u04a9\3\2\2\2\u04a9\u00d3\3\2"+
		"\2\2\u04aa\u04a8\3\2\2\2\u04ab\u04ae\7D\2\2\u04ac\u04ad\7\13\2\2\u04ad"+
		"\u04af\7)\2\2\u04ae\u04ac\3\2\2\2\u04ae\u04af\3\2\2\2\u04af\u04bc\3\2"+
		"\2\2\u04b0\u04bd\7F\2\2\u04b1\u04bd\7N\2\2\u04b2\u04bd\7A\2\2\u04b3\u04b4"+
		"\7M\2\2\u04b4\u04bd\5\u00e4s\2\u04b5\u04b6\7@\2\2\u04b6\u04bd\5\u011c"+
		"\u008f\2\u04b7\u04b8\7\'\2\2\u04b8\u04b9\7\66\2\2\u04b9\u04bd\5\u008c"+
		"G\2\u04ba\u04bb\7E\2\2\u04bb\u04bd\5\u008cG\2\u04bc\u04b0\3\2\2\2\u04bc"+
		"\u04b1\3\2\2\2\u04bc\u04b2\3\2\2\2\u04bc\u04b3\3\2\2\2\u04bc\u04b5\3\2"+
		"\2\2\u04bc\u04b7\3\2\2\2\u04bc\u04ba\3\2\2\2\u04bd\u00d5\3\2\2\2\u04be"+
		"\u04bf\7\22\2\2\u04bf\u04c0\7M\2\2\u04c0\u04c2\5\u00e8u\2\u04c1\u04c3"+
		"\5\u00f0y\2\u04c2\u04c1\3\2\2\2\u04c2\u04c3\3\2\2\2\u04c3\u04c5\3\2\2"+
		"\2\u04c4\u04c6\7\7\2\2\u04c5\u04c4\3\2\2\2\u04c5\u04c6\3\2\2\2\u04c6\u00d7"+
		"\3\2\2\2\u04c7\u04c8\7\22\2\2\u04c8\u04c9\7@\2\2\u04c9\u04ca\5\u011c\u008f"+
		"\2\u04ca\u00d9\3\2\2\2\u04cb\u04cc\7\t\2\2\u04cc\u04cd\7M\2\2\u04cd\u04cf"+
		"\5\u00e4s\2\u04ce\u04d0\5\u00eex\2\u04cf\u04ce\3\2\2\2\u04cf\u04d0\3\2"+
		"\2\2\u04d0\u04d2\3\2\2\2\u04d1\u04d3\7W\2\2\u04d2\u04d1\3\2\2\2\u04d2"+
		"\u04d3\3\2\2\2\u04d3\u04d5\3\2\2\2\u04d4\u04d6\7U\2\2\u04d5\u04d4\3\2"+
		"\2\2\u04d5\u04d6\3\2\2\2\u04d6\u04d8\3\2\2\2\u04d7\u04d9\5\u00ecw\2\u04d8"+
		"\u04d7\3\2\2\2\u04d8\u04d9\3\2\2\2\u04d9\u04db\3\2\2\2\u04da\u04dc\5\u00f0"+
		"y\2\u04db\u04da\3\2\2\2\u04db\u04dc\3\2\2\2\u04dc\u00db\3\2\2\2\u04dd"+
		"\u04de\7\27\2\2\u04de\u04df\7M\2\2\u04df\u04e1\5\u00e4s\2\u04e0\u04e2"+
		"\7\17\2\2\u04e1\u04e0\3\2\2\2\u04e1\u04e2\3\2\2\2\u04e2\u00dd\3\2\2\2"+
		"\u04e3\u04e4\7\27\2\2\u04e4\u04e5\7@\2\2\u04e5\u04e6\5\u011c\u008f\2\u04e6"+
		"\u00df\3\2\2\2\u04e7\u04eb\7#\2\2\u04e8\u04ec\5\u00f2z\2\u04e9\u04ec\5"+
		"\u00f4{\2\u04ea\u04ec\5\u00f6|\2\u04eb\u04e8\3\2\2\2\u04eb\u04e9\3\2\2"+
		"\2\u04eb\u04ea\3\2\2\2\u04ec\u00e1\3\2\2\2\u04ed\u04f1\7?\2\2\u04ee\u04f2"+
		"\5\u00f8}\2\u04ef\u04f2\5\u00fa~\2\u04f0\u04f2\5\u00fc\177\2\u04f1\u04ee"+
		"\3\2\2\2\u04f1\u04ef\3\2\2\2\u04f1\u04f0\3\2\2\2\u04f2\u00e3\3\2\2\2\u04f3"+
		"\u04f6\5\u011c\u008f\2\u04f4\u04f6\5\u0118\u008d\2\u04f5\u04f3\3\2\2\2"+
		"\u04f5\u04f4\3\2\2\2\u04f6\u00e5\3\2\2\2\u04f7\u04f8\7$\2\2\u04f8\u04f9"+
		"\5\u00eav\2\u04f9\u00e7\3\2\2\2\u04fa\u04fb\5\u011c\u008f\2\u04fb\u04fd"+
		"\5\u00e6t\2\u04fc\u04fe\7U\2\2\u04fd\u04fc\3\2\2\2\u04fd\u04fe\3\2\2\2"+
		"\u04fe\u0500\3\2\2\2\u04ff\u0501\5\u00ecw\2\u0500\u04ff\3\2\2\2\u0500"+
		"\u0501\3\2\2\2\u0501\u0506\3\2\2\2\u0502\u0503\5\u0118\u008d\2\u0503\u0504"+
		"\7T\2\2\u0504\u0506\3\2\2\2\u0505\u04fa\3\2\2\2\u0505\u0502\3\2\2\2\u0506"+
		"\u00e9\3\2\2\2\u0507\u0508\7\r\2\2\u0508\u0509\5\u0118\u008d\2\u0509\u00eb"+
		"\3\2\2\2\u050a\u050b\7;\2\2\u050b\u050c\7.\2\2\u050c\u050d\5\u0114\u008b"+
		"\2\u050d\u00ed\3\2\2\2\u050e\u0510\5\u00e6t\2\u050f\u0511\7V\2\2\u0510"+
		"\u050f\3\2\2\2\u0510\u0511\3\2\2\2\u0511\u00ef\3\2\2\2\u0512\u0513\7\5"+
		"\2\2\u0513\u0514\t\f\2\2\u0514\u00f1\3\2\2\2\u0515\u0516\5\u011a\u008e"+
		"\2\u0516\u0517\7I\2\2\u0517\u0518\5\u00fe\u0080\2\u0518\u00f3\3\2\2\2"+
		"\u0519\u051a\5\u0100\u0081\2\u051a\u051b\7I\2\2\u051b\u051c\5\u011c\u008f"+
		"\2\u051c\u00f5\3\2\2\2\u051d\u051e\5\u0104\u0083\2\u051e\u051f\7\66\2"+
		"\2\u051f\u0520\5\u0106\u0084\2\u0520\u0521\7I\2\2\u0521\u0522\5\u011c"+
		"\u008f\2\u0522\u00f7\3\2\2\2\u0523\u0524\5\u011a\u008e\2\u0524\u0525\7"+
		"!\2\2\u0525\u0526\5\u00fe\u0080\2\u0526\u00f9\3\2\2\2\u0527\u0528\5\u0100"+
		"\u0081\2\u0528\u0529\7!\2\2\u0529\u052a\5\u011c\u008f\2\u052a\u00fb\3"+
		"\2\2\2\u052b\u052c\5\u0104\u0083\2\u052c\u052d\7\66\2\2\u052d\u052e\5"+
		"\u0106\u0084\2\u052e\u052f\7!\2\2\u052f\u0530\5\u011c\u008f\2\u0530\u00fd"+
		"\3\2\2\2\u0531\u0532\7M\2\2\u0532\u0536\5\u00e4s\2\u0533\u0534\7@\2\2"+
		"\u0534\u0536\5\u011c\u008f\2\u0535\u0531\3\2\2\2\u0535\u0533\3\2\2\2\u0536"+
		"\u00ff\3\2\2\2\u0537\u053c\5\u0102\u0082\2\u0538\u0539\7j\2\2\u0539\u053b"+
		"\5\u0102\u0082\2\u053a\u0538\3\2\2\2\u053b\u053e\3\2\2\2\u053c\u053a\3"+
		"\2\2\2\u053c\u053d\3\2\2\2\u053d\u0101\3\2\2\2\u053e\u053c\3\2\2\2\u053f"+
		"\u0542\5\u011c\u008f\2\u0540\u0542\7S\2\2\u0541\u053f\3\2\2\2\u0541\u0540"+
		"\3\2\2\2\u0542\u0103\3\2\2\2\u0543\u0546\5\u0102\u0082\2\u0544\u0546\7"+
		"\b\2\2\u0545\u0543\3\2\2\2\u0545\u0544\3\2\2\2\u0546\u054e\3\2\2\2\u0547"+
		"\u054a\7j\2\2\u0548\u054b\5\u0102\u0082\2\u0549\u054b\7\b\2\2\u054a\u0548"+
		"\3\2\2\2\u054a\u0549\3\2\2\2\u054b\u054d\3\2\2\2\u054c\u0547\3\2\2\2\u054d"+
		"\u0550\3\2\2\2\u054e\u054c\3\2\2\2\u054e\u054f\3\2\2\2\u054f\u0105\3\2"+
		"\2\2\u0550\u054e\3\2\2\2\u0551\u0552\5\u008cG\2\u0552\u0107\3\2\2\2\u0553"+
		"\u0556\5\u010a\u0086\2\u0554\u0556\5\u010c\u0087\2\u0555\u0553\3\2\2\2"+
		"\u0555\u0554\3\2\2\2\u0556\u0109\3\2\2\2\u0557\u0558\7p\2\2\u0558\u055d"+
		"\5\u010e\u0088\2\u0559\u055a\7j\2\2\u055a\u055c\5\u010e\u0088\2\u055b"+
		"\u0559\3\2\2\2\u055c\u055f\3\2\2\2\u055d\u055b\3\2\2\2\u055d\u055e\3\2"+
		"\2\2\u055e\u0560\3\2\2\2\u055f\u055d\3\2\2\2\u0560\u0561\7q\2\2\u0561"+
		"\u0565\3\2\2\2\u0562\u0563\7p\2\2\u0563\u0565\7q\2\2\u0564\u0557\3\2\2"+
		"\2\u0564\u0562\3\2\2\2\u0565\u010b\3\2\2\2\u0566\u0567\7n\2\2\u0567\u056c"+
		"\5\u0110\u0089\2\u0568\u0569\7j\2\2\u0569\u056b\5\u0110\u0089\2\u056a"+
		"\u0568\3\2\2\2\u056b\u056e\3\2\2\2\u056c\u056a\3\2\2\2\u056c\u056d\3\2"+
		"\2\2\u056d\u056f\3\2\2\2\u056e\u056c\3\2\2\2\u056f\u0570\7o\2\2\u0570"+
		"\u0574\3\2\2\2\u0571\u0572\7n\2\2\u0572\u0574\7o\2\2\u0573\u0566\3\2\2"+
		"\2\u0573\u0571\3\2\2\2\u0574\u010d\3\2\2\2\u0575\u0576\7\u008a\2\2\u0576"+
		"\u0577\7k\2\2\u0577\u0578\5\u0110\u0089\2\u0578\u010f\3\2\2\2\u0579\u0581"+
		"\5\u010a\u0086\2\u057a\u0581\5\u010c\u0087\2\u057b\u0581\7\u008a\2\2\u057c"+
		"\u0581\5\u0116\u008c\2\u057d\u0581\7\u0087\2\2\u057e\u0581\7\u0086\2\2"+
		"\u057f\u0581\7\u0085\2\2\u0580\u0579\3\2\2\2\u0580\u057a\3\2\2\2\u0580"+
		"\u057b\3\2\2\2\u0580\u057c\3\2\2\2\u0580\u057d\3\2\2\2\u0580\u057e\3\2"+
		"\2\2\u0580\u057f\3\2\2\2\u0581\u0111\3\2\2\2\u0582\u0583\7\21\2\2\u0583"+
		"\u0584\5\u0118\u008d\2\u0584\u0113\3\2\2\2\u0585\u0586\7\u0088\2\2\u0586"+
		"\u0587\7H\2\2\u0587\u0115\3\2\2\2\u0588\u058a\7\u0083\2\2\u0589\u0588"+
		"\3\2\2\2\u0589\u058a\3\2\2\2\u058a\u058b\3\2\2\2\u058b\u058c\t\r\2\2\u058c"+
		"\u0117\3\2\2\2\u058d\u058e\t\16\2\2\u058e\u0119\3\2\2\2\u058f\u0594\5"+
		"\u011c\u008f\2\u0590\u0591\7j\2\2\u0591\u0593\5\u011c\u008f\2\u0592\u0590"+
		"\3\2\2\2\u0593\u0596\3\2\2\2\u0594\u0592\3\2\2\2\u0594\u0595\3\2\2\2\u0595"+
		"\u011b\3\2\2\2\u0596\u0594\3\2\2\2\u0597\u059b\t\17\2\2\u0598\u0599\7"+
		"\u008d\2\2\u0599\u059b\b\u008f\1\2\u059a\u0597\3\2\2\2\u059a\u0598\3\2"+
		"\2\2\u059b\u011d\3\2\2\2\u009d\u0131\u0134\u0140\u014e\u0151\u0154\u0157"+
		"\u015c\u015f\u0164\u016a\u016e\u0177\u0182\u0185\u018b\u0196\u01ab\u01ae"+
		"\u01b2\u01be\u01c2\u01c6\u01d6\u01e1\u01e5\u01ec\u01f4\u01f8\u01fc\u0201"+
		"\u0206\u020e\u0212\u021c\u0223\u022b\u0231\u0236\u0238\u023e\u0245\u024a"+
		"\u0250\u0254\u0258\u025e\u026b\u0270\u0278\u027c\u0280\u0286\u0296\u029d"+
		"\u02a6\u02a9\u02b9\u02be\u02cf\u02e0\u02e9\u02f1\u02f4\u02f8\u02fc\u02fe"+
		"\u0306\u0327\u032f\u0335\u0344\u034c\u0350\u0356\u035c\u0361\u0365\u036d"+
		"\u036f\u0372\u037f\u0386\u038b\u039c\u03a2\u03a8\u03ac\u03b5\u03b8\u03c1"+
		"\u03c4\u03cb\u03d3\u03db\u03e1\u03e7\u03f0\u03fd\u0400\u0409\u0410\u0422"+
		"\u042b\u0436\u043a\u043c\u0443\u044b\u0452\u0455\u0458\u0462\u0469\u046e"+
		"\u0474\u047d\u0483\u0489\u048e\u049a\u04a1\u04a8\u04ae\u04bc\u04c2\u04c5"+
		"\u04cf\u04d2\u04d5\u04d8\u04db\u04e1\u04eb\u04f1\u04f5\u04fd\u0500\u0505"+
		"\u0510\u0535\u053c\u0541\u0545\u054a\u054e\u0555\u055d\u0564\u056c\u0573"+
		"\u0580\u0589\u0594\u059a";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}