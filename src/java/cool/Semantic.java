package cool;
import java.util.*;
public class Semantic
{
	Map<Integer, List<String> > err_msgs ;
	Map<String,String> wrongt;	
	Map<String,List<AST.method>>cl_methmap;
	Map<String,List<String> >class_fun;
	Map<String,List<String>> passfunref;
	List<AST.class_> classList;
	private boolean errorFlag = false;
	public Map<String, String> map; 
	public Map<String, String> funspec_map; 
	public Map<String, List<String> > graph; 
	public Map<String, List<AST.feature>> class_ast; 
	public Map<String ,String > funref;
	public ScopeTable<Info> scopeTbl;
	public List<String> spec_cl;
	public String curclass;
	public String filename;
	public void reportError(String filename, int lineNo, String error){
		errorFlag = true;
		System.err.println(filename+":"+lineNo+": "+error);
	}
	public void passfunrefput(String x, String y)
	{
		if(!passfunref.containsKey(x))
			passfunref.put(x,new ArrayList<String> ());
		List<String> t=passfunref.get(x);
		t.add(y);
		passfunref.put(x,t);
	}
	public String passfunrefget(String x, String y)
	{
		List<String> t=passfunref.get(x);
		String ret=t.get(0);
		for(int i=1;i<t.size();i++)
		{
			if(isAncestor(t.get(i),y))
				ret=t.get(i);
		}
		return ret;
	}
	public boolean getErrorFlag(){
		return errorFlag;
	}
	public void printmsgs(){
		for (Map.Entry<Integer, List<String> > entry : err_msgs.entrySet()) {
			List<String> ls=entry.getValue();
			for(int i=0;i<ls.size();i++){
	    		reportError(filename, entry.getKey() , ls.get(i));
			}
		}
	}
	public void errmsgsput(int l,String s)
	{
		//Storing the error massages 
		List<String> t=new ArrayList<String>();
		if(!err_msgs.containsKey(l))
			err_msgs.put(l,new ArrayList<String>());
		t=err_msgs.get(l);
		t.add(s);
		err_msgs.put(l,t);
	}

	/*
	Don't change code above this line
	 */

	public void addMethod(AST.method meth){
		//Adding the method to the scopetable
		Info info = new Info();
		info.type=meth.typeid;
		info.flag=true;
		info.formals=meth.formals;
		
		scopeTbl.insert(meth.name+"_m",info);
	}

	public void addArguments(AST.method meth){
		//Checking the arguments of the method 
		List<AST.formal> formals = meth.formals;
		ListIterator<AST.formal> itr=formals.listIterator();

		while(itr.hasNext()){
			AST.formal temp=itr.next();
			if(scopeTbl.lookUpLocal(temp.name+"_a")!=null)
				errmsgsput(temp.lineNo,"Formal parameter "+temp.name+" is multiply defined.");
			else
				scopeTbl.insert(temp.name+"_a",new Info(temp.typeid,false,null));
		}
	}

	public boolean isOverriding(AST.method meth){
		//Checking if the method is the proper overriding of parent class
		Info info = scopeTbl.lookUpGlobal(meth.name+"_m");
		if(!meth.typeid.equals(info.type)){
			errmsgsput(meth.lineNo," In redefined method "+meth.name+" return type "+meth.typeid+" is different from original return type "+info.type+".");
			return false;
		}
		List<AST.formal> f1=meth.formals;
		List<AST.formal> f2=info.formals;

		if(f1.size()!=f2.size()){
			errmsgsput(meth.lineNo," Incompatible number of formal parameters in redefined method "+meth.name+".");
			return false;
		}
		for(int i=0;i<f1.size();i++){
			if(!f1.get(i).typeid.equals(f2.get(i).typeid)){
				errmsgsput(meth.lineNo," In redefined method "+meth.name+" parameter type "+f1.get(i).typeid+" is different from original parameter type "+f2.get(i).typeid+".");
				return false;
			}
		}
		return true;
	}


	class Info{
		//Class for Storing the info of feature
		public String type;
		public boolean flag;
		public List <AST.formal> formals;
		public Info(){}
		public Info(String type, boolean flag, List <AST.formal> formals){
			this.type=type;
			this.flag=flag;
			this.formals=formals;
		}

	}
	public boolean ismethodinfrstpass(String callername,String methname,List<AST.expression> args)
	{

		boolean found=false;
		for (Map.Entry<String,List<AST.method>> entry : cl_methmap.entrySet()){ 
			//Looping over each class and method in each class
			if(found)
				break;
			for(int i=0;i<entry.getValue().size();i++)
			{
				AST.method methnow=entry.getValue().get(i);
				if(methnow.name.equals(methname)&&isAncestor(entry.getKey(),callername))
				{
					checkargs(args,methnow.formals,methname);
					found=true;
					break;

				}
				
				
			}
		}
		return found;
	}
	public void fillScopeTable()
	{
		//Entering the scope table
		scopeTbl.enterScope();
		if(curclass.equals("Object"))
		{
			scopeTbl.insert("abort_m",new Info("Object",true,new ArrayList<AST.formal>()));
			funref.put("abort_m","Object");
			scopeTbl.insert("type_name_m",new Info("String",true,new ArrayList<AST.formal>()));
			funref.put("type_name_m","Object");
			scopeTbl.insert("copy_m",new Info("SELF_TYPE",true,new ArrayList<AST.formal>()));
			funref.put("copy_m","Object");
		}
		else if(curclass.equals("IO"))
		{
			List<AST.formal> temp=new ArrayList<AST.formal>();
			temp.add(new AST.formal("x","String",1));
			scopeTbl.insert("out_string_m",new Info("SELF_TYPE",true,temp));
			funref.put("out_string_m","IO");
			temp=new ArrayList<AST.formal>();
			temp.add(new AST.formal("x","Int",1));
			scopeTbl.insert("out_int_m",new Info("SELF_TYPE",true,temp));
			funref.put("out_int_m","IO");
			scopeTbl.insert("in_string_m",new Info("String",true,new ArrayList<AST.formal>()));
			funref.put("in_string_m","IO");
			scopeTbl.insert("in_int_m",new Info("Int",true,new ArrayList<AST.formal>()));
			funref.put("in_int_m","IO");
		}
		else if(curclass.equals("String"))
		{
			scopeTbl.insert("length_m",new Info("Int",true,new ArrayList<AST.formal>()));
			funref.put("length_m","String");
			List<AST.formal> temp=new ArrayList<AST.formal>();
			temp.add(new AST.formal("s","String",1));
			scopeTbl.insert("concat_m",new Info("String",true,temp));
			funref.put("concat_m","String");
			temp=new ArrayList<AST.formal>();
			temp.add(new AST.formal("i","Int",1));
			temp.add(new AST.formal("l","Int",1));
			scopeTbl.insert("substr_m",new Info("String",true,temp));
			funref.put("substr_m","String");
		}
		if(!spec_cl.contains(curclass))
		{
			//Looping over all the features

			List<AST.feature> curflist = class_ast.get(curclass);

			for(int i=0;i<curflist.size();i++){

				
				if( curflist.get(i) instanceof AST.attr){

					AST.attr temp=(AST.attr)curflist.get(i);
					int label=0;

					if(scopeTbl.lookUpGlobal(temp.name+"_a")!=null)
					{
						if(scopeTbl.lookUpLocal(temp.name+"_a")!=null)
						{
							errmsgsput(temp.lineNo,"Attribute "+temp.name+" is an attribute of an inherited class.");
							
						}
						else{
							errmsgsput(temp.lineNo,"Attribute "+temp.name+" is multiply defined in class.");
							
						}
						label++;
					}
					
					if(!map.containsKey(temp.typeid))
					{	
						errmsgsput(temp.lineNo,"Class "+temp.typeid+" of attribute "+temp.name+" is undefined.");
						label++;
						
					}

				  	if(temp.value instanceof AST.no_expr)
					{
						if(label==0){
							
							scopeTbl.insert(temp.name+"_a",new Info(temp.typeid,false,null));
						}
					}
					else
					{	

						String tmp2=getexprtype(temp.value);

						if(arecompatible(temp.typeid,tmp2))
						{  
							if(label==0){

								
								scopeTbl.insert(temp.name+"_a",new Info(temp.typeid,false,null));	
							}
						}
						else
						{
								errmsgsput(temp.lineNo,"Type "+tmp2+" of assigned expression does not conform to declared type "+temp.typeid+" of identifier "+temp.name+".");
						}
					}
				
				}

				else
				{
					AST.method temp=(AST.method)curflist.get(i);

					 if(scopeTbl.lookUpLocal(temp.name+"_m")!=null){
					 	errmsgsput(temp.lineNo,"Method "+temp.name+" is multiply defined.");
					 	

					 }
					 

					 if(scopeTbl.lookUpGlobal(temp.name+"_m")!=null&&scopeTbl.lookUpLocal(temp.name+"_m")==null)
					 {

					 	if(isOverriding(temp)){

					 		addMethod(temp);
					 		funref.put(temp.name+"_m",curclass);
							}


					 	else{
					 			errmsgsput(temp.lineNo,"Method "+temp.name+" exists in inherited class.");
					 	}
					 }
					
					
					if(scopeTbl.lookUpLocal(temp.name+"_m")==null&&scopeTbl.lookUpGlobal(temp.name+"_m")==null)
					{
						addMethod(temp);
						////
					 	funref.put(temp.name+"_m",curclass);
					}
					scopeTbl.enterScope();
					addArguments(temp);
					getexprtype(temp.body);
					scopeTbl.exitScope();
				}
			}
		}

		if(graph.containsKey(curclass))
		{
		
			List<String> chclass=graph.get(curclass);
			//
			//

			for(int i=0;i<chclass.size();i++)
			{
				//
					curclass=chclass.get(i);
					fillScopeTable();
				}		
		}
		scopeTbl.exitScope();

	}
	public boolean checkargs(List<AST.expression> elst,List<AST.formal> flst,String mname)
	{

		List<AST.expression> f1=elst;
		List<AST.formal> f2=flst;
		
		if(f1.size()!=f2.size()){
			errmsgsput(1,"Method "+mname+" called with wrong number of arguments");
			return false;
		}
		for(int i=0;i<f1.size();i++){
			String tp=getexprtype(f1.get(i));
			if(!f2.get(i).typeid.equals(tp)){
				errmsgsput(1,"In call of method "+mname+" type "+tp+" of parameter "+f2.get(i).name+" does not conform to declared type "+f2.get(i).typeid);
				return false;
			}
		}
		//
		return true;


	}

	public boolean isAncestor(String par,String chi)
	{

	   	
		if(par.equals(chi)){

			return true;
		}
		if(graph.get(par)!=null){
			List<String> im_chi= graph.get(par);
			ListIterator<String> itr=im_chi.listIterator();
			
			while(itr.hasNext())
			{
				String cur_chi=itr.next();
				if(chi.equals(cur_chi)||isAncestor(cur_chi,chi))
				{

					return true;
				}
			}
		}
		//
		return false;

	}
	public boolean checkCycle(){

		for (Map.Entry<String, String> entry : map.entrySet()) {
			String cur=entry.getKey();
			String val=entry.getValue();
			while(map.get(val)!=null)
			{
				if(map.get(val).equals(cur))
					return false;
				else
					val=map.get(val);
			}
			
		}
		return true;

	}
	public String comm_par(String par,String elt)
	{
		//Finding the common least ancestor
		while(par!=null)
			{
				if(isAncestor(par,elt))
					return par;
				else
					par=map.get(par);
			}
			return "something wrong";
	}
	public String getexprtype(AST.expression exp_)
	{
		if(exp_ instanceof AST.assign)
		{	AST.assign exp = (AST.assign) exp_;
			String str2=getexprtype(exp.e1);
			//
			String str1=null;
			if(scopeTbl.lookUpGlobal(exp.name+"_a")!=null)
				 str1=scopeTbl.lookUpGlobal(exp.name+"_a").type;
			if(scopeTbl.lookUpGlobal(exp.name+"_a")==null)
				errmsgsput(exp.lineNo,"Assignment to undeclared variable "+exp.name);
			else if( !arecompatible( str1,str2 ))
			{
				errmsgsput(exp.lineNo,"Type "+str2+" of assigned expression does not conform to declared type "+str1+" of identifier "+exp.name+".");

			}
			exp_.type=str2;
			return str2;
		}
		else if(exp_ instanceof AST.block)
		{
			AST.block exp = (AST.block) exp_;
			List<AST.expression> l= exp.l1;
			ListIterator<AST.expression> itr=l.listIterator();
			String typ=null;
			while(itr.hasNext()){
				typ=getexprtype(itr.next());
			}
			exp_.type=typ;
			return typ;
		}
		else if(exp_ instanceof AST.loop)
		{
			AST.loop exp = (AST.loop) exp_;
			if(!getexprtype(exp.predicate).equals("Bool"))
			{

				errmsgsput(exp.lineNo,"Loop condition does not have type Bool.");
			}
			getexprtype(exp.body);
			exp_.type="Object";
			return "Object";

		}
		else if(exp_ instanceof AST.cond){
			AST.cond exp = (AST.cond) exp_;
			if(!getexprtype(exp.predicate).equals("Bool")){

				errmsgsput(exp.lineNo,"Predicate of 'if' does not have type Bool.");
			}

			String ift=getexprtype(exp.ifbody);
			String elt=getexprtype(exp.elsebody);			
			String par=ift;
			while(par!=null)
			{
				if(isAncestor(par,elt)){
					exp_.type=par;
					return par;
				}
				else
					par=map.get(par);
			}
		}
		else if(exp_ instanceof AST.let)
		{
			AST.let exp = (AST.let) exp_;
			scopeTbl.enterScope();
			scopeTbl.insert(exp.name+"_a",new Info(exp.typeid,false,null));
			String str1=exp.typeid;
			String str2=getexprtype(exp.value);
			if(!str2.equals("no_type")){
				if(!arecompatible(str1,str2))
					errmsgsput(exp.lineNo,"Type "+exp.typeid+" of assigned expression does not conform to declared type "+str1+" of identifier "+str2+".");
			}
			String string= getexprtype(exp.body);
			scopeTbl.exitScope();
			exp_.type=string;
			return string;



		}
		else if(exp_ instanceof AST.typcase){
			AST.typcase exp= (AST.typcase)exp_;
			List<AST.branch> blist = exp.branches;
			
			getexprtype(exp.predicate);
			scopeTbl.enterScope();
			scopeTbl.insert(blist.get(0).name+"_a",new Info(blist.get(0).type,false,null));
			String ret=getexprtype(blist.get(0).value);
			scopeTbl.exitScope();
			for(int i=1;i<blist.size();i++)
			{

				scopeTbl.enterScope();
				scopeTbl.insert(blist.get(i).name+"_a",new Info(blist.get(i).type,false,null));
				String txt=getexprtype(blist.get(i).value);
				ret=comm_par(ret,txt);
				scopeTbl.exitScope();

			}
			exp_.type=ret;
		return ret;

	}	
		else if(exp_ instanceof AST.dispatch){
			AST.dispatch exp = (AST.dispatch) exp_;
			String methname=exp.name;
			String callername=getexprtype(exp.caller);
			List<AST.expression> args=exp.actuals;

			
			/*for (Map.Entry<String, String> entry : funref.entrySet())
			{
				
			}
			*/

			//

			if(callername.equals("Object")||callername.equals("self")){

				callername=curclass;
				//
				if(!passfunref.containsKey(methname))
				{
					
					//
					errmsgsput(exp.lineNo,"Dispatch to undefined method "+methname);
				}
				else
				{
					String funclass=passfunrefget(methname,callername);
					if(!isAncestor(funclass,callername)){
						errmsgsput(exp.lineNo,"Dispatch to undefined method "+methname);
					}

					List<AST.method> methods_ =cl_methmap.get(passfunrefget(methname,callername));
					AST.method method=null;
					for(int i=0;i<methods_.size();i++)
						if(methods_.get(i).name.equals(methname))
							method=methods_.get(i);
					if(!checkargs(args,method.formals,methname))
						{

						}
				}
					String retty="SELF_TYPE";
					AST.method function=null;
					String clnm=new String(callername);
					//
					while(!clnm.equals("Object"))
					{
						List<AST.method> functions= cl_methmap.get(clnm);
						for(int i=0;i<functions.size();i++){
							if(functions.get(i).name.equals(methname)){
								function= functions.get(i);
								clnm="Object";
								retty=function.typeid;
								break;
							}
					}
					if(!clnm.equals("Object"))
						clnm=map.get(clnm);
					}
					exp_.type=retty;
					return retty;

			}
			else
			{
				String retty="SELF_TYPE";
				
				
				if(ismethodinfrstpass(callername,methname,args))
				{
					
					AST.method function=null;
					String clnm=new String(callername);
					//
					while(!clnm.equals("Object"))
					{
						List<AST.method> functions= cl_methmap.get(clnm);
						for(int i=0;i<functions.size();i++){
							if(functions.get(i).name.equals(methname)){
								function= functions.get(i);
								clnm="Object";
								retty=function.typeid;
								break;
							}
					}
					if(!clnm.equals("Object"))
						clnm=map.get(clnm);
					}
				}
				else
				{
					errmsgsput(exp.lineNo,"Dispatch to undefined method  "+methname);
				}
				exp_.type=retty ;
				return retty;
			}

			

		}
		else if(exp_ instanceof AST.static_dispatch){
			AST.static_dispatch exp = (AST.static_dispatch) exp_;
			String methname=exp.name;
			String callername=exp.typeid;
			String theB=getexprtype(exp.caller);
			if(!isAncestor(callername,theB))
			{
				errmsgsput(exp.lineNo,"B is not a inherited of typeid");
			}
			List<AST.expression> args=exp.actuals;
			

			if(callername.equals("Object")||callername.equals("self")){

				callername=curclass;
				if(!passfunref.containsKey(methname))
				{
					//
					//
					errmsgsput(exp.lineNo,"Dispatch to undefined method  "+methname);
				}
				else
				{
					String funclass=passfunrefget(methname,callername);
					if(!isAncestor(funclass,callername)){
						errmsgsput(exp.lineNo,"Dispatch to undefined method "+methname);
					}
					List<AST.method> methods_ =cl_methmap.get(passfunrefget(methname,callername));
					AST.method method=null;
					for(int i=0;i<methods_.size();i++)
						if(methods_.get(i).name.equals(methname))
							method=methods_.get(i);
					if(!checkargs(args,method.formals,methname))
						{

						}
				}
				
					String retty="SELF_TYPE";
					AST.method function=null;
					String clnm=new String(callername);
					//
					while(!clnm.equals("Object"))
					{
						List<AST.method> functions= cl_methmap.get(clnm);
						for(int i=0;i<functions.size();i++){
							if(functions.get(i).name.equals(methname)){
								function= functions.get(i);
								clnm="Object";
								retty=function.typeid;
								break;
							}
					}
					if(!clnm.equals("Object"))
						clnm=map.get(clnm);
					}
					exp_.type=retty ;
					return retty;

			}
			else
			{
				String retty="SELF_TYPE";

				if(ismethodinfrstpass(callername,methname,args))
				{
					
					AST.method function=null;
					String clnm=new String(callername);
					//
					while(!clnm.equals("Object"))
					{
						List<AST.method> functions= cl_methmap.get(clnm);
						for(int i=0;i<functions.size();i++){
							if(functions.get(i).name.equals(methname)){
								function= functions.get(i);
								clnm="Object";
								retty=function.typeid;
								break;
							}
					}
					if(!clnm.equals("Object"))
						clnm=map.get(clnm);
					}
				}
				else
				{
					errmsgsput(exp.lineNo,"Dispatch to undefined method  "+methname);
				}

				exp_.type=retty ;
				return retty;
			}

			

			

		}
		else if(exp_ instanceof AST.bool_const){
			exp_.type= "Bool";
				
			return "Bool";

		}
		else if(exp_ instanceof AST.string_const){
			exp_.type= "String";
				
			return "String";

		}
		else if(exp_ instanceof AST.int_const){
			exp_.type="Int" ;
				
			return "Int";
		}
		else if(exp_ instanceof AST.comp){
			AST.comp exp = (AST.comp) exp_;
			if(!getexprtype(exp.e1).equals("Bool")){
				errmsgsput(exp.lineNo,"Expected Bool value in predicate");
			}
			exp_.type="Bool" ;
			return "Bool";
		}
		else if(exp_ instanceof AST.eq){

			AST.eq exp= (AST.eq) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);

	
			if(!(s1.equals("Int")&&s2.equals("Int")||s1.equals("String")&&s2.equals("String")||s1.equals("Bool")&&s2.equals("Bool"))){
				errmsgsput(exp.lineNo,"Illegal comparision with basic type");
			}
			
			exp_.type="Bool" ;
			return "Bool";

	
		}
		else if(exp_ instanceof AST.leq){
			AST.leq exp = (AST.leq) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);
			if(!(s1.equals("Int")&&s2.equals("Int"))) {
			
				errmsgsput(exp.e2.lineNo,"Non Int arguments :"+s1+" <= "+s2);
			}
			
				exp_.type="Bool" ;
				return "Bool";

		}
		else if(exp_ instanceof AST.lt){
			AST.lt exp = (AST.lt) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);
			if(!(s1.equals("Int")&&s2.equals("Int"))) {
			
				errmsgsput(exp.e2.lineNo,"Non Int arguments :"+s1+" < "+s2);
			}
				exp_.type="Bool" ;
				
				return "Bool";


		}
		else if(exp_ instanceof AST.neg){
			AST.neg exp = (AST.neg) exp_;
			if(!getexprtype(exp.e1).equals("Int")){
				errmsgsput(exp.lineNo,"Expected Int value in predicate");
			}
			exp_.type="Int" ;
				
			return "Int";

		}
		else if(exp_ instanceof AST.divide){
			AST.divide exp = (AST.divide) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);
			if(!(s1.equals("Int")&&s2.equals("Int"))) {
			
				errmsgsput(exp.e2.lineNo,"Non Int arguments :"+s1+" / "+s2);
			}
	
				return "Int";

		}
		else if(exp_ instanceof AST.mul){
			AST.mul exp = (AST.mul) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);
			if(!(s1.equals("Int")&&s2.equals("Int"))) {
			
				errmsgsput(exp.e2.lineNo,"Non Int arguments :"+s1+" * "+s2);
			}
				exp_.type= "Int";
				return "Int";
		}
		else if(exp_ instanceof AST.sub){
			AST.sub exp = (AST.sub) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);
			if(!(s1.equals("Int")&&s2.equals("Int"))) {
			
				errmsgsput(exp.e2.lineNo,"Non Int arguments :"+s1+" - "+s2);
			}
		
			exp_.type= "Int";
				return "Int";

		}
		else if(exp_ instanceof AST.plus){
			AST.plus exp = (AST.plus) exp_;
			String s1=getexprtype(exp.e1);
			String s2=getexprtype(exp.e2);
			if(!(s1.equals("Int")&&s2.equals("Int"))) {
			
				errmsgsput(exp.e2.lineNo,"Non Int arguments :"+s1+" + "+s2);
			}
			exp_.type="Int" ;
				
				return "Int";

		}
		else if(exp_ instanceof AST.isvoid){
			AST.isvoid exp = (AST.isvoid) exp_;
			getexprtype(exp.e1);
			exp_.type= "Bool";
				
			return "Bool";
		}
		else if(exp_ instanceof AST.new_){
			AST.new_ exp = (AST.new_) exp_;
			if(map.get(exp.typeid)==null){
				errmsgsput(exp.lineNo,"'new' used with undefined class "+exp.typeid);
			}
			exp_.type=exp.typeid ;
				
			return exp.typeid;
		}
		else if(exp_ instanceof AST.no_expr){
			exp_.type="_no_type" ;
				
			return "_no_type";

		}
		else if(exp_ instanceof AST.object){
			AST.object exp = (AST.object) exp_;
			if(scopeTbl.lookUpGlobal(exp.name+"_a")!=null)
				return scopeTbl.lookUpGlobal(exp.name+"_a").type;

		}
		//

		return "self";
	}
	
	public boolean arecompatible(String lt,String rt)
	{
		
		return isAncestor(lt,rt);
	}


	public void addspecialmeth(){	
		List <AST.method> featlist=new ArrayList<AST.method>();

		passfunrefput("abort","Object");
		passfunrefput("type_name","Object");
		passfunrefput("copy","Object");
		passfunrefput("out_string","IO");
		passfunrefput("out_int","IO");
		passfunrefput("in_string","IO");
		passfunrefput("in_int","IO");
		passfunrefput("concat","String");
		passfunrefput("substr","String");
		passfunrefput("length","String");
		featlist.add(new AST.method("abort",new ArrayList<AST.formal>(),"Object",new AST.expression(),1));
		featlist.add(new AST.method("type_name",new ArrayList<AST.formal>(),"String",new AST.expression(),1));
		featlist.add(new AST.method("copy",new ArrayList<AST.formal>(),"SELF_TYPE",new AST.expression(),1));
		cl_methmap.put("Object",featlist);

		List<AST.formal> temp=new ArrayList<AST.formal>();
		temp.add(new AST.formal("x","String",1));
		featlist=new ArrayList<AST.method>();
		featlist.add(new AST.method("out_string",temp,"SELF_TYPE",new AST.expression(),1));
		temp=new ArrayList<AST.formal>();
		temp.add(new AST.formal("x","Int",1));
		featlist.add(new AST.method("out_int",temp,"SELF_TYPE",new AST.expression(),1));
		featlist.add(new AST.method("in_string",new ArrayList<AST.formal>(),"String",new AST.expression(),1));
		featlist.add(new AST.method("in_int",new ArrayList<AST.formal>(),"String",new AST.expression(),1));
		cl_methmap.put("IO",featlist);
		

		temp=new ArrayList<AST.formal>();
		temp.add(new AST.formal("s","String",1));
		featlist=new ArrayList<AST.method>();
		featlist.add(new AST.method("concat",temp,"String",new AST.expression(),1));
		temp=new ArrayList<AST.formal>();
		temp.add(new AST.formal("i","Int",1));
		temp.add(new AST.formal("l","Int",1));
		featlist.add(new AST.method("substr",temp,"String",new AST.expression(),1));		
		featlist.add(new AST.method("length",new ArrayList<AST.formal>(),"Int",new AST.expression(),1));
		cl_methmap.put("String",featlist);		

	}

	public void firstpass()
	{
		addspecialmeth();
		for(int i=0;i<classList.size();i++)
		{
			List<AST.method> mlst=new ArrayList<AST.method>();
			AST.class_ classh=classList.get(i);
			List<AST.feature> flst=classh.features;
			for(int j=0;j<flst.size();j++){
				if(flst.get(j) instanceof AST.method)
				{
					AST.method met=(AST.method)flst.get(j);
					mlst.add(met);
					passfunrefput(met.name,classh.name);

				}
			}
			cl_methmap.put(classh.name,mlst);
		}
	}



	public boolean classhasmethod(String c,String m)
	{
		for (int i=0;i<cl_methmap.get(c).size();i++)
		{
			if(cl_methmap.get(c).get(i).name.equals(m))
				return true;
		}
		return false;
	}
		
	public Semantic(AST.program program){
		//Write Semantic analyzer code here
		//Initialising the class variables
		cl_methmap=new HashMap <String,List<AST.method>>();
		err_msgs= new TreeMap<Integer,List<String>>();
		funref = new HashMap<String, String>();
		class_ast=new HashMap<String,List<AST.feature>>();
		map = new HashMap<String, String>();
		funspec_map = new HashMap<String, String>();
		passfunref = new HashMap<String,List< String>>();
		graph = new HashMap<String, List<String> >();
		class_fun= new HashMap<String, List<String> >();
		scopeTbl=new ScopeTable<Info>();
		curclass=new String("Object");
		spec_cl=new ArrayList<String>();
		//Adding the special classes
		spec_cl.add("Object");
		spec_cl.add("String");
		spec_cl.add("Int");
		spec_cl.add("IO");
		spec_cl.add("Bool");

	 	classList= new ArrayList<AST.class_>();
		classList=program.classes;
		filename = new String(classList.get(0).filename);

		ListIterator<AST.class_> itr=classList.listIterator(); 
		//Creating the inheritence graph of classes
		while(itr.hasNext()){
			AST.class_ curclass=itr.next();
			if(curclass.name.equals("Object")||curclass.name.equals("IO")||curclass.name.equals("Int")||curclass.name.equals("String")||curclass.name.equals("Bool"))
			{
				errmsgsput(1,"Class "+curclass+" cannot be redefined .");
			}
			if(curclass.parent.equals("Bool")||curclass.parent.equals("Int")||curclass.parent.equals("String"))  
			{
				
				errmsgsput(1,"Class "+curclass.name+" cannot inherit "+curclass.name+".");
			}
			//Checking for multiple classes
			if(map.containsKey(curclass.name))
			{
				errmsgsput(curclass.lineNo,"Multiple Classes with same name");
			}
			else
			{
				//Main class should have main method
				boolean a=true;
				if(curclass.name.equals("Main"))
				{
					a=false;
					for(int i=0;i<curclass.features.size();i++)
					{
						if(curclass.features.get(i) instanceof AST.method)
						{
							AST.method txtr=(AST.method)curclass.features.get(i) ;
							if(txtr.name.equals("main")){
								a=true;
							}
						}
					}
					if(!a)
					{
						reportError(filename,1,"No 'main' method in class Main.");
						return;

					}

				}
				if(a)
				{
					map.put(curclass.name,curclass.parent);
					class_ast.put(curclass.name,curclass.features);
				}


			}
		}

		map.put("String","Object");
		map.put("Int","Object");
		map.put("IO","Object");
		map.put("Bool","Object");
		map.put("Object","God");
		if(!map.containsKey("Main"))
		{
			reportError(filename,1,"Class Main is not defined.");
			return;	
		}
		for (Map.Entry<String, String> entry : map.entrySet())
{
    
}
		/*
		 * TODO: REPORT ERRORS FOR CYCLES
		 */

		if(!checkCycle()){

			reportError(filename,1,"Syntax error at or near CLASS [cyclic inheritance]");
			return;	
		
		}
		for (Map.Entry<String, String> entry : map.entrySet())
		{
			if(graph.containsKey(entry.getValue()))
			{
				List<String> values=graph.get(entry.getValue());
				values.add(entry.getKey());
				graph.put(entry.getValue(),values);
			}
			else
			{
				List<String> values=new ArrayList<String>();
				values.add(entry.getKey());
				graph.put(entry.getValue(),values);
			}
		}

		firstpass();
		//debug();
		fillScopeTable();
		printmsgs();

	}
	public void debug()
	{
		boolean found=false;
		for (Map.Entry<String, List<AST.method> > entry : cl_methmap.entrySet()){ 
			for(int i=0;i<entry.getValue().size();i++)
			{
				//
			}
		}
	}
}