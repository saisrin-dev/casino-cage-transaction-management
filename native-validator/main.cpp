#include <cstdint>
#include <iostream>
#include <future>
#include <vector>
#include <string>
#include <stdexcept>
struct Transaction {std::int64_t grossCents;std::int64_t cashCents;std::string type;};
bool valid(const Transaction& t){return t.grossCents>0&&t.grossCents<=99999999999LL&&t.cashCents>=0&&t.cashCents<=t.grossCents&&(t.type=="MARKER"||t.type=="FRONT_MONEY"||t.type=="CHIP");}
bool ctr(std::int64_t in,std::int64_t out){return in>1000000||out>1000000;}
int main(int argc,char** argv){
 if(argc==2&&std::string(argv[1])=="--test"){
  if(ctr(1000000,0)||ctr(700000,600000)||!ctr(1000001,0)||valid({100,101,"MARKER"})||valid({0,0,"MARKER"})||valid({100,0,"INVALID"}))return 1;
  std::vector<std::future<bool>> tasks;for(int i=0;i<100;i++)tasks.push_back(std::async(std::launch::async,[i]{return valid({10000+i,5000,"FRONT_MONEY"});}));for(auto& t:tasks)if(!t.get())return 1;
  std::cout<<"Boundary checks and 100 concurrent validations passed\n";return 0;
 }
 if(argc!=4){std::cerr<<"Usage: cage-validator GROSS_CENTS CASH_CENTS MARKER|FRONT_MONEY|CHIP\n";return 2;}
 try{std::size_t a,b;const auto gross=std::stoll(argv[1],&a),cash=std::stoll(argv[2],&b);if(a!=std::string(argv[1]).size()||b!=std::string(argv[2]).size())throw std::invalid_argument("integer required");bool ok=valid({gross,cash,argv[3]});std::cout<<(ok?"VALID":"INVALID")<<" — demonstration validation only\n";return ok?0:1;}catch(const std::exception&){std::cerr<<"Amounts must be signed 64-bit integer cents\n";return 2;}
}
